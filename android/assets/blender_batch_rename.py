#!/usr/bin/python

import bpy


def main():
    print("\nStarting level export...")

    from_name = "house_0_window"
    to_name = "wall_outside_window_0"

    # from_name = "house_0_outside_wall_0"
    # to_name = "wall_outside_0"

    # from_name = "house_0_inside_wall_0"
    # to_name = "wall_inside_0"

    # from_name = "house_0_outside_wall_door"
    # to_name = "wall_outside_door_0"

    # from_name = "house_0_inside_wall_door"
    # to_name = "wall_inside_door_0"

    for obj in bpy.data.objects:
        name_array = str(obj.name).split(".")
        if name_array[0] == from_name:
            if len(name_array)==1:
                new_name = to_name
                pass
            else:
                new_name = to_name + "".join(name_array[1:-1]) + "." + name_array[-1]
            obj.name = new_name


if __name__ == "__main__":
    main()
