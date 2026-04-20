Decocraft JSON Entry Documentation

This documents every field available in the decocraft JSON config system.
All blocks, items, chains, and decorations are defined through JSON entries
in files like items.json, seasonal.json, chains.json, etc.

These files are referenced from decocraft.json and groups.json.


# Table of Contents

- [Block Entry Fields](#block-entry-fields)
  - [Required Fields](#required-fields)
  - [Model and Rendering](#model-and-rendering)
  - [Block Behavior](#block-behavior)
  - [Minimal Example](#minimal-example)
- [Block Types](#block-types)
- [Script System](#script-system)
  - [Script Fields](#script-fields)
  - [Actions](#actions)
- [Animations](#animations)
  - [Animation Transitions](#animation-transitions)
  - [Animation End Chains](#animation-end-chains)
  - [Full Animated Example](#full-animated-example)
- [Sounds](#sounds)
  - [Sound Registration](#sound-registration)
  - [Sound Triggers](#sound-triggers)
  - [Sound Fields](#sound-fields)
- [Chain System](#chain-system)
  - [Basic Chain](#basic-chain)
  - [Chain Fields](#chain-fields)
  - [Multiple Model Chain](#multiple-model-chain)
  - [Chain with Flipbook](#chain-with-flipbook)
  - [Chain with Per Segment Materials](#chain-with-per-segment-materials)
  - [Chain Curve Behavior](#chain-curve-behavior)
- [Flipbook Animations](#flipbook-animations-texture-animations)
  - [Flipbook Fields](#flipbook-fields)
- [Composite Models](#composite-models)
  - [Composite Fields](#composite-fields)
- [Storage Blocks](#storage-blocks)
- [Model Switching](#model-switching-variant-cycling)
- [Hidden Variants](#hidden-variants)
- [Growable Structures / Saplings](#growable-structures--saplings)
- [Particles](#particles-snowstorm-system)
  - [Setting Up Particles](#setting-up-particles)
  - [Particle Events](#particle-events)
  - [Billboard / Facing Modes](#billboard--facing-modes)
- [Display Slots](#display-slots)
- [Bed Blocks](#bed-blocks)
- [Seat Blocks](#seat-blocks)
- [File Structure](#file-structure)
- [Dynamic Resource Generation](#dynamic-resource-generation)
- [Examples](#examples)
  - [Simple Decoration](#simple-decoration)
  - [Decoration with Transparency](#decoration-with-transparency)
  - [Decoration with Custom Collision](#decoration-with-custom-collision)
  - [Passable Block](#passable-block)
  - [Water Block](#water-block)
  - [Light Emitting Block](#light-emitting-block)
  - [Toggle Lamp with Hidden On State](#toggle-lamp-with-hidden-on-state)
  - [Animated Block with Click Interaction](#animated-block-with-click-interaction)
  - [Animated Block with Looping Sounds](#animated-block-with-looping-sounds)
  - [Animated Block with Random State](#animated-block-with-random-state)
  - [Jukebox](#jukebox)
  - [Jukebox with Composite Disc](#jukebox-with-composite-disc)
  - [Storage Block Small](#storage-block-small)
  - [Storage Block Large](#storage-block-large)
  - [Doorbell with Sound](#doorbell-with-sound)
  - [Model Switch Cycle (Paintings)](#model-switch-cycle-paintings)
  - [Bed](#bed)
  - [Seat](#seat)
  - [Simple Chain](#simple-chain)
  - [Chain with Lights and Flipbook](#chain-with-lights-and-flipbook)
  - [Flipbook TV](#flipbook-tv)
  - [Flipbook Firepit](#flipbook-firepit)
  - [Growable Sapling](#growable-sapling)
  - [Instant Growth Sapling](#instant-growth-sapling)
  - [Block with Particles](#block-with-particles)
  - [Custom Loot Drop](#custom-loot-drop)
  - [Crafting Bench](#crafting-bench)
  - [Decomposer](#decomposer)
  - [Display Shelf](#display-shelf)
  - [Displayable Item](#displayable-item)


# Block Entry Fields

Every entry lives inside a "models" array in a config JSON file.

## Required Fields

    "name"       - Display name shown in game (String)
    "material"   - Unique texture/material identifier (String)
    "decoref"    - Unique block ID, used for registration (String)
    "tabs"       - Creative tab group name (String), e.g. "clutter", "lighting", "hobby"
    "crafting_color" - RGB color array for crafting display (float[3])

## Model and Rendering

    "model"         - BBModel file name without extension (String)
                      NOT used for chain type entries
    "scale"         - Model scale multiplier (float, default: 1.0)
    "transparency"  - Enable transparency rendering (boolean, default: false)
    "culling"       - Enable face culling (boolean, default: true)

## Block Behavior

    "type"          - Block type, controls which handler class is used (String)
                      See Block Types section below
    "shape"         - Collision shape identifier (String)
    "passable"      - Player can walk through the block (boolean, default: false)
    "above_water"   - Block floats above water (boolean, default: false)
    "hidden"        - Not visible in creative tabs (boolean, default: false)
                      Hidden blocks still exist, used for state variants
    "loot"          - Override drop item decoref (String)
                      If not set, block drops itself
    "displayable"   - Item can be placed into display slots (boolean, default: false)
                      See Display Slots section below

## Minimal Example

    {
      "name": "Table Lamp",
      "model": "table_lamp",
      "material": "table_lamp_white",
      "scale": 1.0,
      "tabs": "lighting",
      "crafting_color": [200, 200, 200],
      "decoref": "table_lamp_white"
    }


# Block Types

The "type" field determines what kind of block is created.
If omitted, defaults to a basic decorative block.

    "underlayer"       - Basic decoration block (default)
    "animated"         - Block with BBModel animations
    "bed"              - Bed block, supports sleeping
    "seat"             - Sittable block
    "jukebox"          - Record player, plays music discs
    "water"            - Water interaction block
    "decobench"        - Decocraft crafting bench
    "nature_bench"     - Nature variant crafting bench
    "decomposer"       - Decomposer/composter block
    "sprouting_table"  - Sprouting variant
    "fake_block"       - Invisible collision block
    "rotatable"        - Block that places at 45 degree increments based on player look
    "chain"            - Chain/vine connector (creates an item, not a block)

Type is also detected automatically in some cases:
- If script.on_use.storage is set -> DecoStorageBlock
- If structures array is present -> GrowableBlock
- If composite.model is set -> DecoAnimatedBlock
- If BBModel has display_ groups with locators -> DecoAnimatedBlock


# Script System

The "script" object contains all interactive behavior.

    {
      "script": {
        "on_use": { ... },
        "shift_on_use": { ... },
        "added": { ... },
        "removed": { ... },
        "animation_start": { ... },
        "animation_end": { ... },
        "tool_modelswitch": { ... },
        "light": 15,
        "particles": { ... }
      }
    }

## Script Fields

    "light"     - Light emission level, 0 to 15 (int, default: 0)
    "particles" - Map of particle effect definitions (see Particles section)

## Actions

Each action (on_use, shift_on_use, added, removed, animation_start,
animation_end, tool_modelswitch) is an Action object with these fields:

    "link"       - Decoref of another block to switch to (String)
    "sound"      - Sound event to play (String), maps to sounds.json
    "animations" - Array of animation transitions (Animation[])
    "sounds"     - Array of sound triggers synced to animations (Sound[])
    "storage"    - Inventory dimensions [width, height] (int[2])


# Animations

Animated blocks need type "animated" and a default_animation field.

    "type": "animated",
    "default_animation": "animation.cuckoo_clock.idle",

Animations are driven by the BBModel file. The animation names must match
the animation names defined in Blockbench.

## Animation Transitions

Defined in an action's "animations" array:

    "animations": [
      {
        "from": "animation.cuckoo_clock.idle",
        "to": "animation.cuckoo_clock.tick"
      },
      {
        "from": "animation.cuckoo_clock.tick",
        "to": "animation.cuckoo_clock.cuckoo"
      },
      {
        "from": "animation.cuckoo_clock.cuckoo",
        "to": "animation.cuckoo_clock.idle"
      }
    ]

The "from" field can be "any" to match any current state.
The "to" field can be "any_other" to pick a random different animation.

## Animation End Chains

Use animation_end to automatically transition when an animation completes:

    "animation_end": {
      "animations": [
        {
          "from": "animation.lava_lamp.idle_1",
          "to": "animation.lava_lamp.idle_2"
        }
      ]
    }

## Full Animated Example

    {
      "name": "Cuckoo Clock",
      "model": "cuckoo_clock",
      "material": "cuckoo_clock",
      "scale": 1.0,
      "tabs": "wall_decor",
      "type": "animated",
      "default_animation": "animation.cuckoo_clock.idle",
      "script": {
        "on_use": {
          "animations": [
            { "to": "animation.cuckoo_clock.tick", "from": "animation.cuckoo_clock.idle" },
            { "to": "animation.cuckoo_clock.cuckoo", "from": "animation.cuckoo_clock.tick" },
            { "to": "animation.cuckoo_clock.idle", "from": "animation.cuckoo_clock.cuckoo" }
          ],
          "sounds": [
            { "from": "animation.cuckoo_clock.idle", "to": "animation.cuckoo_clock.tick", "sound": "tick" },
            { "from": "animation.cuckoo_clock.tick", "to": "animation.cuckoo_clock.cuckoo", "sound": "cuckoo" }
          ]
        },
        "animation_end": {
          "sounds": [
            { "from": "animation.cuckoo_clock.tick", "sound": "tick", "loop": true },
            { "from": "animation.cuckoo_clock.cuckoo", "sound": "cuckoo", "loop": true }
          ]
        }
      },
      "crafting_color": [51.88, 29.22, 18.88],
      "decoref": "cuckoo_clock"
    }


# Sounds

## Sound Registration

Sounds are registered in ModuleSounds and defined in sounds.json.
Currently available: "doorbell", "cuckoo", "tick"

## Sound Triggers

Sounds can be triggered two ways:

Simple sound on action:

    "added": {
      "sound": "doorbell"
    }

Sounds synced to animations with from/to matching:

    "sounds": [
      { "from": "animation.idle", "to": "animation.play", "sound": "tick" },
      { "from": "animation.play", "sound": "tick", "loop": true }
    ]

## Sound Fields

    "from"  - Source animation state name (String)
    "to"    - Target animation state name (String, optional)
    "sound" - Sound identifier from sounds.json (String)
    "loop"  - Loop sound while in this state (boolean, default: false)

Sounds play on the server and are broadcast to all nearby players.


# Chain System

Chains are decorative connections between two blocks.
They use type "chain" and do NOT have a model field.

## Basic Chain

    {
      "name": "Rose Vine",
      "type": "chain",
      "material": "rose_vine",
      "decoref": "rose_vine",
      "tabs": "hobby",
      "crafting_color": [34, 120, 50],
      "chain_models": ["rose_vine"]
    }

## Chain Fields

    "chain_models"     - Array of model names for chain segments (String[])
                         Required for chain type
    "chain_materials"  - Texture overrides for each segment (String[], optional)
                         If omitted, all segments use the entry material
    "chain_pattern"    - How models repeat along the chain (String, default: "mirror")
                         "mirror" - reflects the sequence back and forth
                         "repeat" - cycles through the array
    "lighting"         - Light level emitted along chain, 0 to 15 (int, default: 0)
                         Light blocks are placed along the curve

## Multiple Model Chain

Array order determines which model is used at each position.
The pattern field controls how models repeat for longer chains.

    {
      "name": "Wire",
      "type": "chain",
      "material": "wire",
      "decoref": "wire",
      "tabs": "hobby",
      "crafting_color": [50, 50, 50],
      "chain_models": ["wire"]
    }

## Chain with Flipbook

Chains can have animated textures using flipbook data.

    {
      "name": "Wire Lights Test",
      "type": "chain",
      "material": "wire_lights_test",
      "decoref": "wire_lights_test",
      "tabs": "hobby",
      "crafting_color": [255, 200, 50],
      "chain_models": ["wire", "wire_lights_test", "wire"],
      "flipbook": {
        "frametime": 16,
        "images": 2
      },
      "lighting": 15
    }

## Chain with Per Segment Materials

Each segment can use a different texture by providing a matching
chain_materials array.

    {
      "name": "Mixed Vine",
      "type": "chain",
      "material": "vine_green",
      "decoref": "mixed_vine",
      "tabs": "hobby",
      "crafting_color": [40, 150, 40],
      "chain_models": ["vine_segment", "vine_flower", "vine_segment"],
      "chain_materials": ["vine_green", "vine_flower_pink", "vine_green"],
      "chain_pattern": "repeat"
    }

## Chain Model Requirements

Chain segment models should be built centered on (0, 0, 0) in Blockbench,
extending along the X axis. The renderer places each segment centered on
a point along the curve and rotates it by yaw and pitch to follow the
curve direction. Segments are spaced 0.95 blocks apart with each model
being 1.0 block long, creating a slight overlap for seamless connections.

The cube origin does not affect the final geometry position for elements
without rotation, since the pivot offset and cube offset always cancel
out. Origin only matters when the element has its own rotation, because
the rotation pivots around the origin point. For grouped models, child
elements are positioned relative to their parent group origin.

## Chain Curve Behavior

Chains droop naturally between anchor points using a catenary curve.
If the curve would pass through solid blocks, the sag automatically
reduces until the chain clears. Very short chains become nearly straight.


# Flipbook Animations (Texture Animations)

For blocks with animated textures (TVs, fire, etc).
The texture PNG should be a vertical strip of frames.

    "flipbook": {
      "frametime": 4,
      "images": 6
    }

## Flipbook Fields

    "frametime"  - Ticks between frames (int)
    "images"     - Number of frames in the texture strip (int)

The frame height is calculated automatically from the PNG dimensions.
No mcmeta files needed, everything is generated at runtime.


# Composite Models

Composite renders a second model as a child attached to the main model.
Used for things like jukebox discs that animate independently.

    "composite": {
      "model": "jukebox_disc",
      "transparency": true
    }

## Composite Fields

    "model"        - BBModel name for the child model (String)
    "texture"      - Optional texture override (String)
    "transparency" - Enable transparency for composite (boolean, default: false)

If the composite model is animated, duplicate the parent model for the
composite and delete geometry that is not part of the composite. This
preserves UUID references so animations stay in sync.

Having a composite automatically makes the block use DecoAnimatedBlock.


# Storage Blocks

Any block can become a storage container by adding a storage field
to the on_use action.

    "script": {
      "on_use": {
        "storage": [3, 6]
      }
    }

The array is [columns, rows] defining the inventory grid size.


# Model Switching (Variant Cycling)

Use tool_modelswitch to cycle between block variants with a tool.

    "script": {
      "tool_modelswitch": {
        "link": "painting_1_b"
      }
    }

Each variant links to the next one, forming a cycle.
The last variant links back to the first.


# Hidden Variants

Use hidden blocks for state variants that players should not
directly obtain (open doors, pressed buttons, lit lamps, etc).

    {
      "name": "Table Lamp White On",
      "model": "table_lamp",
      "material": "table_lamp_white_on",
      "hidden": true,
      "script": {
        "light": 15,
        "on_use": { "link": "table_lamp_white" }
      },
      "decoref": "table_lamp_white_on"
    }

Hidden blocks automatically drop their base variant.
The base variant is found by stripping suffixes like _on, _open, _pressed, etc.
You can override the drop with the "loot" field.


# Growable Structures / Saplings

The model field is the sapling appearance.
grows_into is an array of structure file names (random selection).

    {
      "name": "Example Sapling",
      "model": "example_sapling",
      "material": "example_sapling",
      "scale": 1,
      "tabs": "trees",
      "crafting_color": [50.0, 205.0, 50.0],
      "decoref": "test_sapling",
      "grows_into": ["test_struct"]
    }

Structure files go in: resources/data/decocraft/structures

Set "instant": true for immediate growth instead of gradual.


# Particles (Snowstorm System)

Decocraft uses a Snowstorm compatible particle system.
Particles are defined in separate JSON files and referenced from
block entries via locators in the BBModel.

## Setting Up Particles

1. Create particle effect in Snowstorm (snowstorm.app)
2. Save as (particle_name).particle.json
3. Place the particle JSON in: assets/decocraft/particles/
4. Place the particle texture in: assets/decocraft/textures/particle/
5. Add a locator in your BBModel named to match the particle
6. Register the texture in: assets/minecraft/atlases/particles.json

    {
      "sources": [
        {
          "type": "single",
          "resource": "decocraft:particle/embers"
        },
        {
          "type": "single",
          "resource": "decocraft:particle/shower_water"
        }
      ]
    }

The texture field in your particle JSON must use the decocraft namespace:

    "texture": "decocraft:particle/embers"

## Particle Events

Particles support events that fire on expiration or collision.
Events can spawn secondary particle effects.

Define events at the top level of particle_effect:

    "events": {
      "splash": {
        "particle_effect": {
          "effect": "snowstorm:shower_pop",
          "type": "emitter"
        }
      }
    }

Reference events in collision settings:

    "minecraft:particle_motion_collision": {
      "enabled": true,
      "expire_on_contact": true,
      "events": ["splash"]
    }

## Billboard / Facing Modes

The facing_camera_mode field controls how particles orient in 3D space.
All standard Snowstorm/Bedrock modes are supported:

Camera facing (billboard):
    "rotate_xyz"    - Always faces camera (default)
    "rotate_y"      - Faces camera horizontally, stays upright
    "lookat_xyz"    - Points at camera position
    "lookat_y"      - Points at camera position, Y axis only

Direction based (velocity aligned):
    "lookat_direction" - Aligns to travel direction, faces camera
    "direction_x"      - X axis along direction
    "direction_y"      - Y axis along direction
    "direction_z"      - Z axis along direction

Emitter locked (fixed to emitter orientation):
    "emitter_transform_xy" - Locked to emitter XY plane
    "emitter_transform_xz" - Locked to emitter XZ plane (flat horizontal)
    "emitter_transform_yz" - Locked to emitter YZ plane


# Display Slots

Display slots let players place small items onto blocks like shelves and
tables. Slots are defined by locators in the BBModel, items opt in with
the displayable field.

## Setting Up Display Slots

1. In Blockbench, create a group named with a display_ prefix
   (e.g. display_node_0, display_node_1)
2. Add a locator inside each group to mark the position
3. The locator name does not matter, only the group name
4. No JSON fields are needed on the block itself

Blocks with display_ groups are automatically promoted to
DecoAnimatedBlock with a tile entity.

## Making Items Displayable

Add "displayable": true to any item entry.
Only items with this flag can be placed into display slots.

    {
      "name": "Alarm Clock Red",
      "model": "alarm_clock",
      "material": "alarm_clock_red",
      "scale": 1.0,
      "tabs": "clutter",
      "crafting_color": [200, 40, 40],
      "decoref": "alarm_clock_red",
      "displayable": true
    }

## Interaction

Right click a display slot block while holding a displayable item to
place it at the nearest slot. The item faces toward the player.

Shift right click to remove the nearest placed item back to inventory.

If all display slots are full, right click falls through to the block's
normal on_use behavior (animation cycling, etc).

Breaking the block drops all display items as entities.

## Display Slot Naming Convention

Follows the same pattern as sitting_node and sleeping_node:

    display_node_0/     <- group (name starts with display_)
        0               <- locator (any name, provides position)
    display_node_1/
        1
    display_node_2/
        2


# Bed Blocks

    {
      "name": "Day Bed White",
      "model": "day_bed",
      "material": "day_bed_white",
      "type": "bed",
      "tabs": "bedroom",
      "shape": "day_bed_hb",
      "crafting_color": [240, 240, 240],
      "decoref": "day_bed_white"
    }

Bed blocks support cosmetic sleeping. Players can sleep without
advancing time. Respawn point is set when sleeping.


# Seat Blocks

    {
      "name": "Chair Oak",
      "model": "chair",
      "material": "chair_oak",
      "type": "seat",
      "tabs": "seating",
      "crafting_color": [150, 120, 80],
      "decoref": "chair_oak"
    }

Right click to sit. The seat position is determined by the
model's locator points.


# File Structure

    assets/decocraft/
        decocraft.json          - Root config, references other JSON files
        groups.json             - Creative tab definitions
        items.json              - Main block/item definitions
        seasonal.json           - Seasonal content definitions
        chains.json             - Chain definitions
        sounds.json             - Sound event definitions
        models/bbmodel/         - Blockbench model files (.bbmodel)
        textures/block/         - Block textures
        textures/item/          - Item textures
        textures/particle/      - Particle textures
        particles/              - Snowstorm particle definitions
        lang/en_us.json         - Language strings

    data/decocraft/
        structures/             - Structure files for growable blocks
        bounding_boxes/         - Custom collision shapes (.bbmodel)


# Dynamic Resource Generation

Blockstates, block models, item models, item definitions, and loot tables
are all generated dynamically at runtime. You do NOT need to create these
files manually. Only provide:

- The JSON config entry (in items.json, seasonal.json, etc.)
- The BBModel file
- Block and item textures
- Particle textures and definitions (if applicable)
- Structure files (if growable)

The dynamic pack handles everything else automatically.
This applies to both the main mod and any addon packs (like the paintings pack).


---


# Examples

Every possible block configuration demonstrated below.
Click any example in the table of contents above to jump to it.


## Simple Decoration

A basic block with no special behavior.

    {
      "name": "Vase Blue",
      "model": "vase",
      "material": "vase_blue",
      "scale": 1.0,
      "tabs": "clutter",
      "crafting_color": [60, 80, 200],
      "decoref": "vase_blue"
    }


## Decoration with Transparency

Glass, bottles, or anything see through.

    {
      "name": "Wine Glass",
      "model": "wine_glass",
      "material": "wine_glass",
      "scale": 1.0,
      "transparency": true,
      "tabs": "food",
      "crafting_color": [200, 200, 220],
      "decoref": "wine_glass"
    }


## Decoration with Custom Collision

Use a shape ID to define a custom hitbox from a bounding box bbmodel file.

    {
      "name": "Filing Cabinet Birch",
      "model": "filing_cabinet",
      "material": "filing_cabinet_birch",
      "scale": 1.0,
      "shape": "filing_cabinet_birch",
      "tabs": "storage",
      "crafting_color": [180, 150, 100],
      "decoref": "filing_cabinet_birch"
    }


## Passable Block

Players can walk through this block.

    {
      "name": "Flower Pot",
      "model": "flower_pot",
      "material": "flower_pot_red",
      "passable": true,
      "tabs": "clutter",
      "crafting_color": [180, 60, 40],
      "decoref": "flower_pot_red"
    }


## Water Block

Block that floats on water surfaces.

    {
      "name": "Lily Pad Deco",
      "model": "lily_pad",
      "material": "lily_pad",
      "type": "water",
      "above_water": true,
      "tabs": "clutter",
      "crafting_color": [40, 150, 40],
      "decoref": "lily_pad_deco"
    }


## Light Emitting Block

Block that gives off light using the script light field.

    {
      "name": "Candle White",
      "model": "candle",
      "material": "candle_white",
      "tabs": "lighting",
      "script": {
        "light": 12
      },
      "crafting_color": [240, 240, 230],
      "decoref": "candle_white"
    }


## Toggle Lamp with Hidden On State

Two entries that link to each other. The "on" state is hidden
so it does not appear in the creative tab.

Off state (visible in creative tab):

    {
      "name": "Table Lamp White",
      "model": "table_lamp",
      "material": "table_lamp_white",
      "tabs": "lighting",
      "script": {
        "on_use": { "link": "table_lamp_white_on" }
      },
      "crafting_color": [240, 240, 240],
      "decoref": "table_lamp_white"
    }

On state (hidden, emits light, clicks back to off):

    {
      "name": "Table Lamp White On",
      "model": "table_lamp",
      "material": "table_lamp_white_on",
      "hidden": true,
      "script": {
        "light": 15,
        "on_use": { "link": "table_lamp_white" }
      },
      "decoref": "table_lamp_white_on"
    }


## Animated Block with Click Interaction

A block that cycles through animations when right clicked.

    {
      "name": "Newton Cradle",
      "model": "newtons_cradle",
      "material": "newtons_cradle",
      "scale": 1.0,
      "tabs": "hobby",
      "type": "animated",
      "default_animation": "animation.newtons_cradle.idle",
      "script": {
        "on_use": {
          "animations": [
            { "from": "animation.newtons_cradle.idle", "to": "animation.newtons_cradle.play" },
            { "from": "animation.newtons_cradle.play", "to": "animation.newtons_cradle.idle" }
          ]
        }
      },
      "crafting_color": [31.48, 33.43, 35.07],
      "decoref": "newtons_cradle"
    }


## Animated Block with Looping Sounds

Sounds that play each time an animation loops.

    {
      "name": "Cuckoo Clock",
      "model": "cuckoo_clock",
      "material": "cuckoo_clock",
      "scale": 1.0,
      "tabs": "wall_decor",
      "type": "animated",
      "default_animation": "animation.cuckoo_clock.idle",
      "script": {
        "on_use": {
          "animations": [
            { "from": "animation.cuckoo_clock.idle", "to": "animation.cuckoo_clock.tick" },
            { "from": "animation.cuckoo_clock.tick", "to": "animation.cuckoo_clock.cuckoo" },
            { "from": "animation.cuckoo_clock.cuckoo", "to": "animation.cuckoo_clock.idle" }
          ],
          "sounds": [
            { "from": "animation.cuckoo_clock.idle", "to": "animation.cuckoo_clock.tick", "sound": "tick" },
            { "from": "animation.cuckoo_clock.tick", "to": "animation.cuckoo_clock.cuckoo", "sound": "cuckoo" }
          ]
        },
        "animation_end": {
          "sounds": [
            { "from": "animation.cuckoo_clock.tick", "sound": "tick", "loop": true },
            { "from": "animation.cuckoo_clock.cuckoo", "sound": "cuckoo", "loop": true }
          ]
        }
      },
      "crafting_color": [51.88, 29.22, 18.88],
      "decoref": "cuckoo_clock"
    }


## Animated Block with Random State

Using "any_other" to randomly pick a different animation each cycle.

    {
      "name": "Lava Lamp",
      "model": "lava_lamp",
      "material": "lava_lamp_red",
      "scale": 1.0,
      "tabs": "lighting",
      "type": "animated",
      "default_animation": "animation.lava_lamp.idle_1",
      "script": {
        "light": 10,
        "animation_end": {
          "animations": [
            { "from": "any", "to": "any_other" }
          ]
        }
      },
      "crafting_color": [200, 40, 40],
      "decoref": "lava_lamp_red"
    }


## Jukebox

A record player block that plays music discs.

    {
      "name": "Jukebox",
      "model": "jukebox",
      "material": "jukebox",
      "scale": 1.0,
      "tabs": "hobby",
      "type": "jukebox",
      "default_animation": "animation.jukebox_idle",
      "script": {
        "on_use": {
          "animations": [
            { "from": "animation.jukebox_idle", "to": "animation.jukebox_playing" },
            { "from": "animation.jukebox_playing", "to": "animation.jukebox_idle" }
          ]
        }
      },
      "crafting_color": [38.71, 33.42, 27.86],
      "decoref": "jukebox"
    }


## Jukebox with Composite Disc

A jukebox with a separate animated disc model on top.

    {
      "name": "Jukebox Deluxe",
      "model": "jukebox",
      "material": "jukebox_deluxe",
      "scale": 1.0,
      "transparency": true,
      "tabs": "hobby",
      "type": "jukebox",
      "default_animation": "animation.jukebox_idle",
      "composite": {
        "model": "jukebox_disc"
      },
      "script": {
        "on_use": {
          "animations": [
            { "from": "animation.jukebox_idle", "to": "animation.jukebox_playing" },
            { "from": "animation.jukebox_playing", "to": "animation.jukebox_idle" }
          ]
        }
      },
      "crafting_color": [38.71, 33.42, 27.86],
      "decoref": "jukebox_deluxe"
    }


## Storage Block Small

A block with a small inventory (chest, drawer, etc).

    {
      "name": "Drawer Oak",
      "model": "drawer",
      "material": "drawer_oak",
      "scale": 1.0,
      "tabs": "storage",
      "script": {
        "on_use": {
          "storage": [3, 3]
        }
      },
      "crafting_color": [150, 120, 80],
      "decoref": "drawer_oak"
    }


## Storage Block Large

A block with a large inventory.

    {
      "name": "Closet Oak",
      "model": "closet",
      "material": "closet_oak",
      "scale": 1.0,
      "tabs": "storage",
      "script": {
        "on_use": {
          "storage": [9, 6]
        }
      },
      "crafting_color": [150, 120, 80],
      "decoref": "closet_oak"
    }


## Doorbell with Sound

Plays a sound when placed, then switches to a hidden pressed variant.

Base doorbell:

    {
      "name": "Doorbell White",
      "model": "doorbell",
      "material": "doorbell_white",
      "tabs": "wall_decor",
      "script": {
        "on_use": {
          "link": "doorbell_pressed_white",
          "sound": "doorbell"
        }
      },
      "crafting_color": [240, 240, 240],
      "decoref": "doorbell_white"
    }

Pressed state (hidden, links back):

    {
      "name": "Doorbell Pressed White",
      "model": "doorbell_pressed",
      "material": "doorbell_pressed_white",
      "hidden": true,
      "script": {
        "on_use": { "link": "doorbell_white" }
      },
      "decoref": "doorbell_pressed_white"
    }


## Model Switch Cycle (Paintings)

A set of blocks that cycle through variants when interacted with using
the decobrush tool. Each links to the next in a loop.

    {
      "name": "Painting 1 A",
      "model": "painting_1",
      "material": "painting_1_a",
      "scale": 1.0,
      "tabs": "paintings",
      "type": "underlayer",
      "crafting_color": [35, 30, 25],
      "decoref": "painting_1_a",
      "script": {
        "tool_modelswitch": { "link": "painting_1_b" }
      }
    },
    {
      "name": "Painting 1 B",
      "model": "painting_1",
      "material": "painting_1_b",
      "scale": 1.0,
      "tabs": "paintings",
      "type": "underlayer",
      "crafting_color": [35, 32, 25],
      "decoref": "painting_1_b",
      "script": {
        "tool_modelswitch": { "link": "painting_1_a" }
      }
    }


## Bed

    {
      "name": "Day Bed White",
      "model": "day_bed",
      "material": "day_bed_white",
      "type": "bed",
      "tabs": "bedroom",
      "shape": "day_bed_hb",
      "crafting_color": [240, 240, 240],
      "decoref": "day_bed_white"
    }


## Seat

    {
      "name": "Chair Oak",
      "model": "chair",
      "material": "chair_oak",
      "type": "seat",
      "tabs": "seating",
      "crafting_color": [150, 120, 80],
      "decoref": "chair_oak"
    }


## Simple Chain

    {
      "name": "Rose Vine",
      "type": "chain",
      "material": "rose_vine",
      "decoref": "rose_vine",
      "tabs": "hobby",
      "crafting_color": [34, 120, 50],
      "chain_models": ["rose_vine"]
    }


## Chain with Lights and Flipbook

A chain with alternating models, animated texture, and light emission.

    {
      "name": "Wire Lights Test",
      "type": "chain",
      "material": "wire_lights_test",
      "decoref": "wire_lights_test",
      "tabs": "hobby",
      "crafting_color": [255, 200, 50],
      "chain_models": ["wire", "wire_lights_test", "wire"],
      "flipbook": {
        "frametime": 16,
        "images": 2
      },
      "lighting": 15
    }


## Flipbook TV

A block with an animated screen texture.

    {
      "name": "TV Small",
      "model": "tv_small",
      "material": "tv_small_static_flipbook",
      "scale": 1.0,
      "tabs": "hobby",
      "flipbook": {
        "frametime": 4,
        "images": 4
      },
      "crafting_color": [40, 40, 40],
      "decoref": "tv_small_static_flipbook"
    }


## Flipbook Firepit

A fire effect using animated textures.

    {
      "name": "Firepit",
      "model": "firepit",
      "material": "firepit",
      "scale": 1.0,
      "tabs": "seasonal",
      "script": {
        "light": 15
      },
      "flipbook": {
        "frametime": 4,
        "images": 6
      },
      "crafting_color": [200, 100, 30],
      "decoref": "firepit"
    }


## Growable Sapling

A sapling that grows into a random structure over time.

    {
      "name": "Example Sapling",
      "model": "example_sapling",
      "material": "example_sapling",
      "scale": 1,
      "tabs": "trees",
      "crafting_color": [50, 205, 50],
      "decoref": "test_sapling",
      "grows_into": ["test_struct", "test_struct_2"]
    }


## Instant Growth Sapling

Grows immediately when placed instead of over time.

    {
      "name": "Magic Tree",
      "model": "magic_sapling",
      "material": "magic_sapling",
      "scale": 1,
      "tabs": "trees",
      "crafting_color": [100, 50, 200],
      "decoref": "magic_sapling",
      "grows_into": ["magic_tree"],
      "instant": true
    }


## Block with Particles

A block that spawns Snowstorm particles from a locator in the model.
The locator in the BBModel must be named to match the particle file.

    {
      "name": "Firepit with Embers",
      "model": "firepit",
      "material": "firepit",
      "scale": 1.0,
      "tabs": "seasonal",
      "script": {
        "light": 15
      },
      "flipbook": {
        "frametime": 4,
        "images": 6
      },
      "crafting_color": [200, 100, 30],
      "decoref": "firepit"
    }

The particle is loaded automatically when the BBModel contains a locator
folder named after a particle JSON file in assets/decocraft/particles/.


## Custom Loot Drop

A block that drops a different item when broken.

    {
      "name": "Closet Oak Open",
      "model": "closet_open",
      "material": "closet_oak_open",
      "hidden": true,
      "loot": "closet_oak",
      "script": {
        "on_use": { "link": "closet_oak" }
      },
      "decoref": "closet_oak_open"
    }


## Crafting Bench

    {
      "name": "Decobench",
      "model": "decobench",
      "material": "decobench",
      "scale": 1.0,
      "type": "decobench",
      "tabs": "clutter",
      "crafting_color": [120, 90, 60],
      "decoref": "decobench"
    }


## Decomposer

    {
      "name": "Decomposer",
      "model": "decomposer",
      "material": "decomposer",
      "scale": 1.0,
      "type": "decomposer",
      "tabs": "clutter",
      "crafting_color": [80, 100, 60],
      "decoref": "decomposer"
    }


## Display Shelf

A block with display slots. The BBModel contains display_ groups
with locators. No special JSON fields needed on the block entry.

    {
      "name": "Bamboo Table Green",
      "model": "bamboo_table",
      "material": "bamboo_table_green",
      "scale": 1.0,
      "tabs": "seasonal",
      "crafting_color": [34.94, 46.94, 18.1],
      "decoref": "bamboo_table_green"
    }

The display slots come from the BBModel file, not the JSON.
Any block whose BBModel has display_ groups gets display slots automatically.


## Displayable Item

An item that can be placed into display slots.

    {
      "name": "Alarm Clock Red",
      "model": "alarm_clock",
      "material": "alarm_clock_red",
      "scale": 1.0,
      "tabs": "clutter",
      "crafting_color": [200, 40, 40],
      "decoref": "alarm_clock_red",
      "displayable": true
    }

The item renders as its full BBModel at the locator position.
Items are placed facing toward the player.
