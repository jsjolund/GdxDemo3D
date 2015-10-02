#!/usr/bin/python

"""
Blender to libGDX export script.

Maps the scene by object type: models, lights, empties and cameras.
All models with unique meshes (models which are not linked by Alt+D) are exported to FBX, then converted to G3DB
with the fbx-conv program (fbx-conv must be in your $PATH). The unique models will be exported to
../g3db/blenderfilename_modelname.g3db. These models, along with any linked non-unique models will have their
position, rotation and scale transform recorded in a JSON file located at ../json/blenderfilename_model.json. In
order for this to work, the unique model to be exported will have its transform reset (position set to [0,0,0],
scale to [1,1,1], etc.). Placing the linked model in the scene can then be done  programmatically by loading the
model, reading the json file, and creating model instances according to the json file.

In order to keep track of which model to use as the reference when exporting, any linked models should follow the
naming convention modelname.number. E.g. a model called "car" will be used for exporting, while linked models named
"car.001", "car.002" etc. will only have their transforms recorded in the json file. The naming convention is not
enforced however. The script actually checks for linked meshes, and if no models without numbering is found, the model
first in Blender's internal scene object data structure will be used. However, if any models "car.001" and "car.002" are
found which do NOT share the same mesh are found, an error is thrown, since exporting both would result in a file name
collision.

Models with an armature modifier will automatically have the armature skinning included in the exported g3db file.

Lights, empties and cameras will not be exported, but only recorded in the json files
../json/blenderfilename_objecttype.json.
"""

import bpy
import os
import math
import subprocess
import json
from mathutils import Vector


class BlenderObject(object):
    def __init__(self, blender_object):
        self.blender_object = blender_object
        self.name = blender_object.name
        self.name_array = str(blender_object.name).split(".")
        self.loc = blender_object.location.copy()
        self.rote = blender_object.rotation_euler.copy()
        self.rot = [math.degrees(a) for a in blender_object.rotation_euler]
        self.scl = blender_object.scale.copy()
        self.type_name = str(self.blender_object.data.__class__.__name__)
        self.custom_properties = {}
        for obj in (o for o in blender_object.keys() if not o in '_RNA_UI'):
            self.custom_properties[obj] = blender_object[obj]
        self.entry = {}

    def serialize(self):
        self.entry["name"] = self.name_array[0]
        self.entry["type"] = self.type_name
        self.entry["position"] = {"x": self.loc.x, "y": self.loc.y, "z": self.loc.z}
        self.entry["rotation"] = {"x": self.rot[0], "y": self.rot[1], "z": self.rot[2]}
        self.entry["scale"] = {"x": self.scl.x, "y": self.scl.y, "z": self.scl.z}
        self.entry["custom_properties"] = self.custom_properties


class BlenderModel(BlenderObject):
    category = "model"

    def __init__(self, blender_object, filename):
        super().__init__(blender_object)
        self.filename = filename

    def get_model_name(self):
        return self.filename + "_" + self.name_array[0]

    def serialize(self):
        super().serialize()
        self.entry["model_file_name"] = self.get_model_name()
        return self.entry


class BlenderEmpty(BlenderObject):
    category = "empty"

    def __init__(self, blender_object):
        super().__init__(blender_object)

    def serialize(self):
        super().serialize()
        return self.entry


class BlenderLight(BlenderObject):
    category = "light"

    def __init__(self, blender_object):
        super().__init__(blender_object)
        self.lamp_color = blender_object.data.color
        self.lamp_energy = blender_object.data.energy
        self.lamp_dst = blender_object.data.distance
        if type(self.blender_object.data) is bpy.types.SpotLamp:
            self.lamp_falloff = self.blender_object.data.spot_size
        else:
            self.lamp_falloff = 0

    def serialize(self):
        super().serialize()
        self.entry["lamp_color"] = {"r": self.lamp_color.r, "g": self.lamp_color.g, "b": self.lamp_color.b, "a": 1.0}
        self.entry["lamp_energy"] = self.lamp_energy
        self.entry["lamp_distance"] = self.lamp_dst
        self.entry["lamp_falloff"] = self.lamp_falloff
        return self.entry


class BlenderCamera(BlenderObject):
    category = "camera"

    def __init__(self, blender_object):
        super().__init__(blender_object)
        self.fov = self.blender_object.data.angle

    def serialize(self):
        super().serialize()
        self.entry["fov"] = math.degrees(self.fov)
        return self.entry


class Color(object):
    def __init__(self):
        self.r = 0.0
        self.g = 0.0
        self.b = 0.0


def write_json(blender_file_basedir, blender_filename_noext, blender_object_map):
    json_dir_path = os.path.join(os.path.dirname(blender_file_basedir), "json")
    if not os.path.exists(json_dir_path):
        os.makedirs(json_dir_path)
    for category in blender_object_map:
        json_file_path = os.path.join(json_dir_path, "{}_{}.json".format(blender_filename_noext, category))
        json_out = []
        for obj in blender_object_map[category]:
            json_out.append(obj.serialize())
        json_file = open(json_file_path, "w")
        json_file.write(json.dumps(json_out))
        json_file.write("\n")
        print("Wrote to " + json_file_path)
        print(json.dumps(json_out, sort_keys=True, indent=4, separators=(',', ': ')))
        json_file.close()


def write_fbx(blender_file_basedir, export_objects):
    fbx_file_paths = []

    # deselect all
    for item in bpy.context.selectable_objects:
        item.select = False

    for game_object in export_objects:
        blender_object = game_object.blender_object
        blender_object.select = True
        # Select any connected armatures
        armatures = []
        for mod in blender_object.modifiers:
            if type(mod) is bpy.types.ArmatureModifier:
                armatures.append(mod.object)
        for armature in armatures:
            armature.select = True
        fbx_file_path = os.path.join(blender_file_basedir, game_object.get_model_name() + ".fbx")
        fbx_file_paths.append(fbx_file_path)

        bpy.context.scene.objects.active = blender_object
        blender_object.location.zero()
        blender_object.rotation_euler.zero()
        blender_object.scale = Vector((1.0, 1.0, 1.0))
        bpy.ops.export_scene.fbx(filepath=fbx_file_path,
                                 version='BIN7400',
                                 path_mode="RELATIVE",
                                 use_selection=True,
                                 use_tspace=True,
                                 use_mesh_modifiers=True,
                                 object_types={'MESH', 'ARMATURE'},
                                 # axis_forward='-Z',
                                 # axis_up='Y',
                                 )
        blender_object.location = game_object.loc.copy()
        blender_object.rotation_euler = game_object.rote.copy()
        blender_object.scale = game_object.scl.copy()

        # Deselect the object and any armatures
        blender_object.select = False
        for armature in armatures:
            armature.select = False

    return fbx_file_paths


def convert_to_g3db(blender_file_basedir, fbx_file_paths):
    g3db_output_file_paths = []
    g3db_output_path = os.path.join(os.path.dirname(blender_file_basedir), "g3db")
    if not os.path.exists(g3db_output_path):
        os.makedirs(g3db_output_path)

    for fbx_file_path in fbx_file_paths:
        basename_ext = os.path.basename(fbx_file_path)
        basename_no_ext, fbx_ext = os.path.splitext(basename_ext)
        g3db_output_file_path = os.path.join(g3db_output_path, basename_no_ext + ".g3db")
        subprocess.call(["fbx-conv", "-f", fbx_file_path, g3db_output_file_path])
        os.remove(fbx_file_path)
        g3db_output_file_paths.append(g3db_output_file_path)
    return g3db_output_file_paths


def get_export_objects(blender_object_map):
    mesh_objects = {}
    export_objects = []

    for obj in blender_object_map[BlenderModel.category]:
        scene_mesh_name = obj.blender_object.data.name
        scene_object_name = obj.name_array[0]
        if scene_object_name in mesh_objects:
            if not scene_mesh_name in mesh_objects.get(scene_object_name):
                mesh_objects.get(scene_object_name).append(scene_mesh_name)
        else:
            mesh_objects[scene_object_name] = [scene_mesh_name]
            export_objects.append(obj)

    for key in mesh_objects:
        if len(mesh_objects[key]) > 1:
            print("Error: Conflicting meshes for export object: '{}'.\nRename objects or link the meshes: {}\n".format(
                key, str(mesh_objects[key])))
            return []

    print("Info: Found export targets:")
    for o in export_objects:
        print(o.name)

    return export_objects


def create_blender_object_map(filename, scene_objects):
    blender_object_map = {}

    for scene_obj in scene_objects:
        if scene_obj.hide:
            continue

        category = "unknown"

        if type(scene_obj.data) is bpy.types.Mesh:
            blender_object = BlenderModel(scene_obj, filename)
            category = BlenderModel.category

        elif scene_obj.data is None:
            blender_object = BlenderEmpty(scene_obj)
            category = BlenderEmpty.category

        elif type(scene_obj.data) in [bpy.types.PointLamp, bpy.types.SpotLamp, bpy.types.SunLamp]:
            blender_object = BlenderLight(scene_obj)
            category = BlenderLight.category

        elif type(scene_obj.data) is bpy.types.Camera:
            blender_object = BlenderCamera(scene_obj)
            category = BlenderCamera.category

        if category == "unknown":
            print("Warning: {} not supported".format(str(scene_obj.data.__class__.__name__)))
            continue

        if category in blender_object_map:
            blender_object_map.get(category).append(blender_object)
        else:
            blender_object_map[category] = [blender_object]

    return blender_object_map


def main():
    print("\nStarting level export...")
    blender_file_basedir = os.path.dirname(bpy.data.filepath)
    blender_filename_noext = bpy.path.basename(bpy.context.blend_data.filepath).split(".")[0]

    if not blender_file_basedir:
        raise Exception("Blend file is not saved")

    bpy.data.scenes['Scene'].render.fps = 30

    blender_object_map = create_blender_object_map(blender_filename_noext, bpy.data.objects)

    print(blender_file_basedir)
    write_json(blender_file_basedir, blender_filename_noext, blender_object_map)

    print()
    export_objects = get_export_objects(blender_object_map);
    print()
    fbx_file_paths = write_fbx(blender_file_basedir, export_objects)
    print()
    g3db_file_paths = convert_to_g3db(blender_file_basedir, fbx_file_paths)
    print("\nCreated g3db model files:")
    for f in g3db_file_paths:
        print(f)
    print("\nFinished.")
    print("\n\n")


if __name__ == "__main__":
    main()
