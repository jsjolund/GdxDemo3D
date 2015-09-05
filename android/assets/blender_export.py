#!/usr/bin/python

"""
Blender to libGDX level export script.
"""

import bpy
import os
import math
import subprocess
import json
from mathutils import Vector

class BlenderObject(object):
    def __init__(self, bobj):
        self.bobj = bobj
        self.name = bobj.name
        self.name_array = str(bobj.name).split(".")
        self.loc = bobj.location.copy()
        self.rote = bobj.rotation_euler.copy()
        self.rot = [math.degrees(a) for a in bobj.rotation_euler]
        self.scl = bobj.scale.copy()
        self.type_name = str(self.bobj.data.__class__.__name__)
        self.custom_properties = {}
        for obj in (o for o in bobj.keys() if not o in '_RNA_UI'):
            self.custom_properties[obj] = bobj[obj]
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

    def __init__(self, bobj, filename):
        super().__init__(bobj)
        self.filename = filename

    def get_model_name(self):
        return self.filename + "_" + self.name_array[0]

    def serialize(self):
        super().serialize()
        self.entry["model_file_name"] = self.get_model_name()
        return self.entry


class BlenderEmpty(BlenderObject):
    category = "empty"

    def __init__(self, bobj):
        super().__init__(bobj)

    def serialize(self):
        super().serialize()
        return self.entry


class BlenderLight(BlenderObject):
    category = "light"

    def __init__(self, bobj):
        super().__init__(bobj)
        self.lamp_color = bobj.data.color
        self.lamp_energy = bobj.data.energy
        self.lamp_dst = bobj.data.distance
        if type(self.bobj.data) is bpy.types.SpotLamp:
            self.lamp_falloff = self.bobj.data.spot_size
        else:
            self.lamp_falloff = 0

    def serialize(self):
        super().serialize()
        self.entry["lamp_color"] = {"r": self.lamp_color.r, "g": self.lamp_color.g, "b": self.lamp_color.b, "a": 1.0}
        self.entry["lamp_energy"] = self.lamp_energy
        self.entry["lamp_distance"] = self.lamp_dst
        self.entry["lamp_falloff"] = self.lamp_falloff
        return self.entry


class BlenderArmature(BlenderObject):
    category = "armature"

    def __init__(self, bobj):
        super().__init__(bobj)

    def serialize(self):
        super().serialize()
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
    obj_file_paths = []

    # unselect all
    for item in bpy.context.selectable_objects:
        item.select = False

    for gobj in export_objects:
        bobj0 = gobj.bobj
        bobj0.select = True
        # Select any connected armatures
        armatures = []
        for mod in bobj0.modifiers:
            if type(mod) is bpy.types.ArmatureModifier:
                armatures.append(mod.object)
        for armature in armatures:
            print("selecting "+ str(armature))
            armature.select = True
        obj_file_path = os.path.join(blender_file_basedir, gobj.get_model_name() + ".fbx")
        obj_file_paths.append(obj_file_path)

        # Select object, set loc & rot to zero, export to obj, restore loc & rot, unselect
        # bobj0.select = True
        bpy.context.scene.objects.active = bobj0
        bobj0.location.zero()
        bobj0.rotation_euler.zero()
        bobj0.scale = Vector((1.0, 1.0, 1.0))
        bpy.ops.export_scene.fbx(filepath=obj_file_path,
                                 path_mode="RELATIVE",
                                 use_selection=True,
                                 use_tspace=True,
                                 use_mesh_modifiers=True,
                                 object_types={'MESH', 'ARMATURE'},
                                 axis_forward='Y',
                                 axis_up='Z')
        bobj0.location = gobj.loc.copy()
        bobj0.rotation_euler = gobj.rote.copy()
        bobj0.scale = gobj.scl.copy()

        # Deselect the object and any armatures
        bobj0.select = False
        for armature in armatures:
            armature.select = False

    return obj_file_paths


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
        scene_mesh_name = obj.bobj.data.name
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
            gobj = BlenderModel(scene_obj, filename)
            category = BlenderModel.category

        elif scene_obj.data is None:
            gobj = BlenderEmpty(scene_obj)
            category = BlenderEmpty.category

        elif type(scene_obj.data) in [bpy.types.PointLamp, bpy.types.SpotLamp, bpy.types.SunLamp]:
            gobj = BlenderLight(scene_obj)
            category = BlenderLight.category

        # elif type(scene_obj.data) is bpy.types.Armature:
        #     gobj = BlenderArmature(scene_obj)
        #     category = BlenderArmature.category

        if category == "unknown":
            print("Warning: {} not supported".format(str(scene_obj.data.__class__.__name__)))
            continue

        if category in blender_object_map:
            blender_object_map.get(category).append(gobj)
        else:
            blender_object_map[category] = [gobj]

    return blender_object_map


def reset_origins():
    # unselect all
    for item in bpy.context.selectable_objects:
        item.select = False
    i = 0
    for obj in bpy.data.objects:
        i += 1
        print("Origin set for object {} out of {}.".format(i, len(bpy.data.objects)))
        obj.select = True
        bpy.context.scene.objects.active = obj
        bpy.ops.object.origin_set(type='ORIGIN_GEOMETRY', center='BOUNDS')
        obj.select = False


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
