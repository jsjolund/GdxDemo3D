#!/usr/bin/python

"""
Copyright 2015 See AUTHORS file.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
"""

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

import os
import math
import subprocess
import json
import tempfile
import sys

import bpy
from mathutils import Vector


class BlenderObject(object):
    def __init__(self, blender_object):
        self.blender_object = blender_object
        self.name = blender_object.name
        self.blender_name = blender_object.name
        self.name_array = str(blender_object.name).split(".")
        self.loc = blender_object.location.copy()
        self.rote = blender_object.rotation_euler.copy()
        self.rot = [math.degrees(a) for a in blender_object.rotation_euler]
        self.scl = blender_object.scale.copy()
        self.type_name = str(self.blender_object.data.__class__.__name__)
        self.layers = bpy.data.objects[blender_object.name].layers[0:20]
        self.custom_properties = {}
        for obj in (o for o in blender_object.keys() if not o in '_RNA_UI'):
            self.custom_properties[obj] = blender_object[obj]
        self.entry = {}

    def serialize(self):
        self.entry["name"] = self.name_array[0]
        self.entry["type"] = self.type_name
        self.entry["layers"] = self.layers
        self.entry["custom_properties"] = self.custom_properties

        # Convert to y-axis up
        self.entry["position"] = {"x": self.loc.x, "y": self.loc.z, "z": -self.loc.y}
        self.entry["rotation"] = {"x": self.rot[0], "y": self.rot[2], "z": -self.rot[1]}
        self.entry["scale"] = {"x": self.scl.x, "y": self.scl.z, "z": self.scl.y}


class BlenderModel(BlenderObject):
    category = "model"

    def __init__(self, blender_object, filename):
        super().__init__(blender_object)
        self.filename = filename

    def get_model_file_name(self):
        return self.filename + "_" + self.name_array[0]

    def get_model_name(self):
        return self.name_array[0]

    def get_mesh_name(self):
        return self.blender_object.data.name

    def serialize(self):
        super().serialize()
        self.entry["model_file_name"] = self.get_model_file_name()
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


class ModelToMeshMap(object):
    def __init__(self):
        self.model_to_mesh_dict = {}
        self.longest_model_name = 0
        self.longest_model_file_name = 0
        self.longest_mesh_name = 0

    def add_model_mesh_object_entry(self, model_name, mesh_name, game_object):
        if model_name in self.model_to_mesh_dict:
            mesh_to_obj_dict = self.model_to_mesh_dict[model_name]
            if mesh_name in mesh_to_obj_dict:
                mesh_to_obj_dict[mesh_name].append(game_object)
            else:
                mesh_to_obj_dict[mesh_name] = [game_object]
        else:
            self.model_to_mesh_dict[model_name] = {mesh_name: [game_object]}

        if len(model_name) > self.longest_model_name:
            self.longest_model_name = len(model_name)
        if len(mesh_name) > self.longest_mesh_name:
            self.longest_mesh_name = len(mesh_name)
        file_name = game_object.get_model_file_name() + ".g3db"
        if len(file_name) > self.longest_model_file_name:
            self.longest_model_file_name = len(file_name)

    def has_conflicting_models_names(self):
        for model_name in self.model_to_mesh_dict:
            mesh_to_obj_dict = self.model_to_mesh_dict[model_name]
            if len(mesh_to_obj_dict) > 1:
                return True
        return False

    def print_conflicting_model_names(self):
        print("ERROR: Conflicting meshes for models:")
        for model_name in self.model_to_mesh_dict:
            mesh_to_obj_dict = self.model_to_mesh_dict[model_name]
            if len(mesh_to_obj_dict) > 1:
                print("Models named {} have conflicting meshes:".format((model_name)))
                for mesh_name in mesh_to_obj_dict:
                    print("\n\nMESH: {}\nLINKED MODELS: ".format((model_name)), end="")
                    for game_object in mesh_to_obj_dict[mesh_name]:
                        print(game_object.blender_name, end=", ")
                print()
        print("Rename the models if they are different, link the meshes (Alt+D) if they are identical.")

    def get_export_targets(self):
        targets = []
        for model_name in self.model_to_mesh_dict:
            mesh_to_obj_dict = self.model_to_mesh_dict[model_name]
            for mesh_name in mesh_to_obj_dict:
                first_game_object_with_name = mesh_to_obj_dict[mesh_name][0]
                targets.append(first_game_object_with_name)
        return targets

    def print_summary(self):
        col_offset = 3
        max_line_width = 0
        print()
        model_header = "MODEL NAME"
        file_name_header = "FILE NAME"
        file_name_header_spacing = self.longest_model_name - len(model_header) + col_offset
        mesh_header = "MESH NAME"
        mesh_header_spacing = self.longest_model_file_name - len(file_name_header) + col_offset
        print(model_header + " " * file_name_header_spacing + file_name_header + " " * mesh_header_spacing + mesh_header)
        for model_name in self.model_to_mesh_dict:
            mesh_to_obj_dict = self.model_to_mesh_dict[model_name]
            spacing = (self.longest_mesh_name - len(model_name) + col_offset)
            output = model_name + spacing * " "
            line_width = len(output)
            for mesh_name in mesh_to_obj_dict:
                game_object = mesh_to_obj_dict[mesh_name][0]
                file_name = game_object.get_model_file_name() + ".g3db"
                spacing = (self.longest_model_file_name - len(file_name) + col_offset)
                if (len(mesh_to_obj_dict)) > 1:
                    if len(output) > line_width:
                        output += "\n" + " " * line_width
                        output += file_name + spacing * " "
                        output += mesh_name
                    else:
                        output += file_name + spacing * " "
                        output += mesh_name
                else:
                    output += file_name + spacing * " "
                    output += mesh_name
            print(output)
        print()


def write_json(json_file_path, objects):
    """
    Serialize the "BlenderObjects" and write them to json file
    :param json_file_path:
    :param objects:
    :return:
    """
    json_out = []
    for obj in objects:
        json_out.append(obj.serialize())
    json_file = open(json_file_path, "w")
    json_file.write(json.dumps(json_out))
    json_file.write("\n")
    print("INFO: Wrote to " + json_file_path)
    json_file.close()


def write_fbx(game_object, fbx_file_path):
    # deselect all
    for item in bpy.context.selectable_objects:
        item.select = False

    blender_object = game_object.blender_object

    # Select object any connected armatures
    blender_object.select = True
    for mod in blender_object.modifiers:
        if type(mod) is bpy.types.ArmatureModifier:
            mod.object.select = True
    bpy.context.scene.objects.active = blender_object

    # Reset location, rotation and scale before exporting
    blender_object.location.zero()
    blender_object.rotation_euler.zero()
    blender_object.scale = Vector((1.0, 1.0, 1.0))
    status = True
    try:
        out = bpy.ops.export_scene.fbx(filepath=fbx_file_path,
                                       version='BIN7400',
                                       path_mode="RELATIVE",
                                       use_selection=True,
                                       use_tspace=True,
                                       use_mesh_modifiers=True,
                                       object_types={'MESH', 'ARMATURE'})
    except RuntimeError:
        status = False

    blender_object.location = game_object.loc.copy()
    blender_object.rotation_euler = game_object.rote.copy()
    blender_object.scale = game_object.scl.copy()

    # Deselect object and any armatures
    blender_object.select = False
    for mod in blender_object.modifiers:
        if type(mod) is bpy.types.ArmatureModifier:
            mod.object.select = False
    return status


def fbx_conv(fbx_file_path, g3db_output_path):
    """
    Convert an fbx file to g3db with fbx-conv
    :param g3db_output_path:
    :param fbx_file_path:
    :return: The path of the g3db file
    """
    basename_ext = os.path.basename(fbx_file_path)
    basename_no_ext, fbx_ext = os.path.splitext(basename_ext)
    g3db_file_path = os.path.join(g3db_output_path, basename_no_ext + ".g3db")
    subprocess.call(["fbx-conv", "-f", fbx_file_path, g3db_file_path])
    return g3db_file_path




def get_export_objects(blender_model_list):
    """
    Get a list of all models which need to be exported (those with unique meshes)
    :param blender_model_list: The models to consider
    :return: The objects which will be exported
    """
    model_to_mesh_map = ModelToMeshMap()
    for obj in blender_model_list:
        model_to_mesh_map.add_model_mesh_object_entry(obj.get_model_name(), obj.get_mesh_name(), obj)

    model_to_mesh_map.print_summary()
    if model_to_mesh_map.has_conflicting_models_names():
        model_to_mesh_map.print_conflicting_model_names()
        return []

    return model_to_mesh_map.get_export_targets()


def create_blender_object_map(filename, scene_objects):
    """
    Map all supported scene objects into "BlenderObjects" which can be exported and/or serialized.
    :param filename:
    :param scene_objects:
    :return:
    """
    blender_object_map = {}

    for scene_obj in scene_objects:
        # Don't include hidden objects
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
            print("WARNING: {} not supported".format(str(scene_obj.data.__class__.__name__)))
            continue

        if category in blender_object_map:
            blender_object_map.get(category).append(blender_object)
        else:
            blender_object_map[category] = [blender_object]

    return blender_object_map


def main():
    print("\n### Starting scene export...")
    blender_file_basedir = os.path.dirname(bpy.data.filepath)
    blender_filename_noext = bpy.path.basename(bpy.context.blend_data.filepath).split(".")[0]

    if not blender_file_basedir:
        raise Exception("ERROR: Blender file is not saved")

    bpy.data.scenes['Scene'].render.fps = 30
    blender_object_map = create_blender_object_map(blender_filename_noext, bpy.data.objects)

    # Write json
    json_dir_path = os.path.join(os.path.dirname(blender_file_basedir), "json")
    if not os.path.exists(json_dir_path):
        os.makedirs(json_dir_path)
    for category in blender_object_map:
        json_file_path = os.path.join(json_dir_path, "{}_{}.json".format(blender_filename_noext, category))
        write_json(json_file_path, blender_object_map[category])
    print()

    export_objects = get_export_objects(blender_object_map[BlenderModel.category])

    if len(export_objects) == 0:
        print("\nFound no export targets.\n### Aborted\n\n")
        return

    # Export models as fbx to temp dir, then convert them to g3db
    with tempfile.TemporaryDirectory() as tmpdirname:
        fbx_file_paths = []
        for game_object in export_objects:
            fbx_file_path = os.path.join(tmpdirname, game_object.get_model_file_name() + ".fbx")
            if not write_fbx(game_object, fbx_file_path):
                print("\nFailed to export model '{}'\n### Aborted\n\n".format(game_object.blender_name))
                return
            fbx_file_paths.append(fbx_file_path)

        g3db_output_path = os.path.join(os.path.dirname(blender_file_basedir), "g3db")
        if not os.path.exists(g3db_output_path):
            os.makedirs(g3db_output_path)

        g3db_file_paths = []
        for fbx_file_path in fbx_file_paths:
            g3db_file_path = fbx_conv(fbx_file_path, g3db_output_path)
            g3db_file_paths.append(g3db_file_path)

        print("\nINFO: Created g3db model files:")
        for g3db in g3db_file_paths:
            print(g3db)
    print("\n### Finished\n\n")


if __name__ == "__main__":
    main()
