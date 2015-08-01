# Creates a CSV file listing the names of each blender object
# in the .blend along with its position and rotation.
# The file has the same name as the .blend file but with .csv
# extension.
#
# For each model which name does NOT contain a number it also
# exports the model to a .obj file for importing into LibGDX.
#
# E.g. 'box.001', 'box.002' would NOT get exported, but 'box'
# will be exported to 'box.obj' and 'box.mtl'.

import bpy
import os
import math
import subprocess
import json
from mathutils import Vector


class Color(object):
    def __init__(self):
        self.r = 0.0
        self.g = 0.0
        self.b = 0.0


class GameObject(object):
    def get_first_name(self):
        return self.name_array[0]

    def get_model_name(self):
        return self.filename + "_" + self.get_first_name()

    def reset_origin(self):
        pass

    def get_attributes(self):
        return [self.get_model_name(), self.get_index(),
                self.loc.x, self.loc.y, self.loc.z,
                self.rot[0], self.rot[1], self.rot[2],
                self.scl.x, self.scl.y, self.scl.z]

    def get_index(self):
        if (len(self.name_array) > 1):
            return int(self.name_array[1])
        else:
            return 0

    def __init__(self, filename, bobj):
        self.filename = filename
        self.bobj = bobj
        self.name_array = str(bobj.name).split(".")
        self.loc = bobj.location.copy()
        self.rote = bobj.rotation_euler.copy()
        self.rot = [math.degrees(a) for a in bobj.rotation_euler]
        self.scl = bobj.scale.copy()
        self.lamp_color = Color()
        self.lamp_energy = 0
        self.lamp_dst = 0
        self.lamp_falloff = 0
        # self.angle
        if self.is_lamp():
            self.lamp_color = bobj.data.color
            self.lamp_energy = bobj.data.energy
            self.lamp_dst = bobj.data.distance
            if type(self.bobj.data) is bpy.types.SpotLamp:
                self.lamp_falloff = self.bobj.data.spot_size

    def is_empty(self):
        return self.bobj.data is None

    def is_mesh(self):
        return type(self.bobj.data) is bpy.types.Mesh

    def is_lamp(self):
        return type(self.bobj.data) in [bpy.types.PointLamp, bpy.types.SpotLamp, bpy.types.SunLamp]

    def serialize(self):
        entry = {}
        entry["name"] = self.get_model_name()
        entry["type"] = str(self.bobj.data.__class__.__name__)

        entry["position"] = {"x": self.loc.x, "y": self.loc.y, "z": self.loc.z}
        entry["rotation"] = {"x": self.rot[0], "y": self.rot[1], "z": self.rot[2]}
        entry["scale"] = {"x": self.scl.x, "y": self.scl.y, "z": self.scl.z}

        entry["lamp_color"] = {"r": self.lamp_color.r, "g": self.lamp_color.g, "b": self.lamp_color.b, "a": 1.0}
        entry["lamp_energy"] = self.lamp_energy
        entry["lamp_distance"] = self.lamp_dst
        entry["lamp_falloff"] = self.lamp_falloff

        return entry


def write_json(json_file_path, gobj_map):
    json_file = open(json_file_path, "w")
    json_out = []
    for name, gobj_list in gobj_map.items():
        for gobj in gobj_list:
            json_out.append(gobj.serialize())
    json_file.write(json.dumps(json_out))
    json_file.write("\n")
    print(json.dumps(json_out, sort_keys=True, indent=4, separators=(',', ': ')))
    json_file.close()


def write_obj(obj_dir, export_objects):
    obj_file_paths = []

    # unselect all
    for item in bpy.context.selectable_objects:
        item.select = False

    for gobj0 in export_objects:
        bobj0 = gobj0.bobj
        obj_file_path = os.path.join(obj_dir, gobj0.get_model_name() + ".obj")
        obj_file_paths.append(obj_file_path)

        # Select object, set loc & rot to zero, export to obj, restore loc & rot, unselect
        bobj0.select = True
        bpy.context.scene.objects.active = bobj0
        bobj0.location.zero()
        bobj0.rotation_euler.zero()
        bobj0.scale = Vector((1.0, 1.0, 1.0))
        bpy.ops.export_scene.obj(filepath=obj_file_path, use_selection=True)
        bobj0.location = gobj0.loc.copy()
        bobj0.rotation_euler = gobj0.rote.copy()
        bobj0.scale = gobj0.scl.copy()
        bobj0.select = False

    return obj_file_paths


def convert_to_g3db(obj_file_paths):
    g3db_file_paths = []
    for obj_file_path in obj_file_paths:
        subprocess.call(["fbx-conv", "-f", obj_file_path])
        file_path_noext, file_ext = os.path.splitext(obj_file_path)
        os.remove(file_path_noext + ".obj")
        os.remove(file_path_noext + ".mtl")
        g3db_file_paths.append(file_path_noext + ".g3db")


def get_export_objects(gobj_map):
    export_objects = []
    for name, gobj_list in gobj_map.items():
        if len(gobj_list) == 0:
            print("WARNING: No instances found for {}, using ".format(name))
        gobj0 = gobj_list[0]
        if not gobj0.is_mesh():
            print("INFO: {} is not a mesh.".format(name))
            continue
        gobj0_candidates = []
        for gobj in gobj_list:
            if gobj.get_index() == 0:
                gobj0_candidates.append(gobj)
        if len(gobj0_candidates) > 1:
            print("WARNING: Multiple base models found for {}, using: {}".format(name, gobj0.bobj.name))
        if len(gobj0_candidates) == 0:
            print("WARNING: No base model found for {}, using: {}".format(name, gobj0.bobj.name))
        export_objects.append(gobj0)
    return export_objects


def create_game_object_map(filename, objects):
    gobj_map = {}
    for obj in objects:
        gobj = GameObject(filename, obj)
        # Create map over names and objects with those names.
        # Allow meshes and empties.
        if gobj.is_mesh() or gobj.is_empty() or gobj.is_lamp():
            name = gobj.get_first_name()
            if name in gobj_map:
                gobj_map.get(name).append(gobj)
            else:
                gobj_map[name] = [gobj]
    return gobj_map


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
    basedir = os.path.dirname(bpy.data.filepath)
    filename = bpy.path.basename(bpy.context.blend_data.filepath).split(".")[0]

    if not basedir:
        raise Exception("Blend file is not saved")

    # reset_origins()
    gobj_map = create_game_object_map(filename, bpy.data.objects)

    json_file_path = os.path.join(basedir, filename + ".json")
    print("\nWriting to " + json_file_path)
    print()
    write_json(json_file_path, gobj_map)

    print()
    export_objects = get_export_objects(gobj_map);
    print()
    obj_file_paths = write_obj(basedir, export_objects)
    print()
    g3db_file_paths = convert_to_g3db(obj_file_paths)
    print("\nFinished.")
    print("\n\n")


if __name__ == "__main__":
    main()
