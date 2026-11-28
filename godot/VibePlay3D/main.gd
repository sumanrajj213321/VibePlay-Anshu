extends Node3D

# Original lightweight 3D adventure prototype for Android.
# Touch the left half of the screen to move; drag the right half to look.

var player: CharacterBody3D
var camera_pivot: Node3D
var camera: Camera3D
var move_touch := -1
var look_touch := -1
var move_origin := Vector2.ZERO
var move_vector := Vector2.ZERO
var look_last := Vector2.ZERO
var yaw := 0.0
var pitch := -0.22
var gravity := 18.0
var speed := 6.0

func _ready() -> void:
	_build_world()
	_build_player()
	_build_hud()

func _build_world() -> void:
	var world_env := WorldEnvironment.new()
	var env := Environment.new()
	env.background_mode = Environment.BG_SKY
	env.sky = Sky.new()
	env.sky.sky_material = ProceduralSkyMaterial.new()
	env.ambient_light_source = Environment.AMBIENT_SOURCE_COLOR
	env.ambient_light_color = Color(0.72, 0.82, 0.95)
	env.ambient_light_energy = 0.8
	world_env.environment = env
	add_child(world_env)

	var sun := DirectionalLight3D.new()
	sun.rotation_degrees = Vector3(-52, -28, 0)
	sun.light_energy = 1.15
	sun.shadow_enabled = true
	add_child(sun)

	var ground := MeshInstance3D.new()
	var plane := PlaneMesh.new()
	plane.size = Vector2(160, 160)
	ground.mesh = plane
	var grass := StandardMaterial3D.new()
	grass.albedo_color = Color(0.19, 0.48, 0.25)
	grass.roughness = 1.0
	ground.material_override = grass
	ground.position.y = -0.12
	add_child(ground)

	var ground_body := StaticBody3D.new()
	var ground_shape := CollisionShape3D.new()
	var box := BoxShape3D.new()
	box.size = Vector3(160, 0.2, 160)
	ground_shape.shape = box
	ground_body.add_child(ground_shape)
	ground_body.position.y = -0.12
	add_child(ground_body)

	# Simple colourful landmarks make the first world feel alive.
	for i in range(12):
		var tree := Node3D.new()
		var trunk := MeshInstance3D.new()
		var trunk_mesh := CylinderMesh.new()
		trunk_mesh.top_radius = 0.22
		trunk_mesh.bottom_radius = 0.32
		trunk_mesh.height = 1.8
		trunk.mesh = trunk_mesh
		var bark := StandardMaterial3D.new()
		bark.albedo_color = Color(0.34, 0.19, 0.10)
		trunk.material_override = bark
		trunk.position.y = 0.9
		tree.add_child(trunk)

		var crown := MeshInstance3D.new()
		var crown_mesh := SphereMesh.new()
		crown_mesh.radius = 1.0
		crown_mesh.height = 1.8
		crown.mesh = crown_mesh
		var leaves := StandardMaterial3D.new()
		leaves.albedo_color = Color(0.12, 0.42 + float(i % 3) * 0.06, 0.20)
		crown.material_override = leaves
		crown.position.y = 2.0
		tree.add_child(crown)
		var angle := TAU * float(i) / 12.0
		tree.position = Vector3(cos(angle) * 12.0, 0, sin(angle) * 12.0)
		add_child(tree)

func _build_player() -> void:
	player = CharacterBody3D.new()
	player.name = "Player"
	player.position = Vector3(0, 1.1, 0)
	add_child(player)

	var collision := CollisionShape3D.new()
	var capsule := CapsuleShape3D.new()
	capsule.radius = 0.42
	capsule.height = 1.8
	collision.shape = capsule
	player.add_child(collision)

	var body := MeshInstance3D.new()
	var body_mesh := CapsuleMesh.new()
	body_mesh.radius = 0.42
	body_mesh.height = 1.8
	body.mesh = body_mesh
	var avatar_mat := StandardMaterial3D.new()
	avatar_mat.albedo_color = Color(0.20, 0.55, 0.95)
	body.material_override = avatar_mat
	player.add_child(body)

	var head := MeshInstance3D.new()
	var head_mesh := SphereMesh.new()
	head_mesh.radius = 0.28
	head_mesh.height = 0.56
	head.mesh = head_mesh
	var skin := StandardMaterial3D.new()
	skin.albedo_color = Color(0.95, 0.72, 0.55)
	head.material_override = skin
	head.position.y = 0.92
	player.add_child(head)

	camera_pivot = Node3D.new()
	camera_pivot.position.y = 0.7
	player.add_child(camera_pivot)
	camera = Camera3D.new()
	camera.position = Vector3(0, 3.0, 6.5)
	camera.rotation.x = -0.32
	camera.current = true
	camera_pivot.add_child(camera)

func _build_hud() -> void:
	var layer := CanvasLayer.new()
	add_child(layer)
	var title := Label.new()
	title.text = "VIBEWORLD  •  Explore together"
	title.position = Vector2(24, 18)
	title.add_theme_font_size_override("font_size", 28)
	title.add_theme_color_override("font_color", Color.WHITE)
	layer.add_child(title)

	var hint := Label.new()
	hint.text = "Touch left side to move  •  Drag right side to look"
	hint.position = Vector2(24, 58)
	hint.add_theme_font_size_override("font_size", 18)
	hint.add_theme_color_override("font_color", Color(0.95, 0.98, 1))
	layer.add_child(hint)

	var badge := Label.new()
	badge.text = "SOLO WORLD  •  MULTIPLAYER COMING NEXT"
	badge.position = Vector2(24, 94)
	badge.add_theme_font_size_override("font_size", 15)
	badge.add_theme_color_override("font_color", Color(1.0, 0.88, 0.48))
	layer.add_child(badge)

func _input(event: InputEvent) -> void:
	if event is InputEventScreenTouch:
		if event.pressed:
			if event.position.x < get_viewport().get_visible_rect().size.x * 0.5 and move_touch == -1:
				move_touch = event.index
				move_origin = event.position
				move_vector = Vector2.ZERO
			elif look_touch == -1:
				look_touch = event.index
				look_last = event.position
		else:
			if event.index == move_touch:
				move_touch = -1
				move_vector = Vector2.ZERO
			if event.index == look_touch:
				look_touch = -1
	elif event is InputEventScreenDrag:
		if event.index == move_touch:
			move_vector = (event.position - move_origin).limit_length(95.0) / 95.0
		elif event.index == look_touch:
			var delta := event.position - look_last
			yaw -= delta.x * 0.006
			pitch = clamp(pitch - delta.y * 0.004, -0.85, 0.25)
			look_last = event.position

func _physics_process(delta: float) -> void:
	if not is_instance_valid(player):
		return
	var input_dir := Vector2.ZERO
	if move_touch != -1:
		input_dir = move_vector
	else:
		input_dir.x = Input.get_action_strength("ui_right") - Input.get_action_strength("ui_left")
		input_dir.y = Input.get_action_strength("ui_down") - Input.get_action_strength("ui_up")
	var forward := Vector3(sin(yaw), 0, cos(yaw))
	var right := Vector3(cos(yaw), 0, -sin(yaw))
	var direction := (right * input_dir.x + forward * input_dir.y).normalized()
	player.velocity.x = direction.x * speed
	player.velocity.z = direction.z * speed
	if not player.is_on_floor():
		player.velocity.y -= gravity * delta
	else:
		player.velocity.y = 0
	player.move_and_slide()
	if direction.length() > 0.1:
		player.rotation.y = lerp_angle(player.rotation.y, atan2(direction.x, direction.z), delta * 10.0)
	camera_pivot.rotation.y = yaw
	camera.rotation.x = pitch
