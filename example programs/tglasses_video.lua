-- Uses monitor from Tom's Peripherals as output https://modrinth.com/mod/toms-peripherals
local port = peripheral.find("goggle_link_port")
local gpu = peripheral.find("tm_gpu")
local chat = peripheral.find("chatBox")

local euler_mode = false
local depth_map = false
local do_cache = false
local show_partial = false

local do_shade_color = false
local shading_offset = 0.0

local width_roll_range = math.rad(45)
local height_pitch_range = math.rad(45)
local max_distance = 60
local new_distance = max_distance
local vector_fov = 1

local colors = {}
for i = 0, 15 do
    colors[i] = string.format("%x", i)
end

local table_insert = table.insert
local function linspace(start, end_, num)
    local linspaced = {}
    if num == 0 then return linspaced end
    if num == 1 then
        table_insert(linspaced, start)
        return linspaced
    end

    local delta = (end_ - start) / (num - 1)

    for i = 0, num-2 do
        table_insert(linspaced, start+delta*i)
    end
    table_insert(linspaced, end_)

    return linspaced
end

local width, height, pixels, xy_pixels

local img1, img2
local cur_img = true

local function get_current_image(comp) comp = comp or cur_img return cur_img and img1 or img2 end
local function toggle_current_image() cur_img = not cur_img end

local function setGPUSize(size)
    if img1 ~= nil then img1.free() end
    if img2 ~= nil then img2.free() end

    gpu.setSize(size)

    gpu.refreshSize()
    gpu.fill()
    gpu.sync()

    width, height = gpu.getSize()
    img1 = gpu.newImage(width, height)
    img2 = gpu.newImage(width, height)

    local x_axis = linspace(-width_roll_range, width_roll_range, width)
    local y_axis = linspace(-height_pitch_range, height_pitch_range, height)

    height_pitch_range = height_pitch_range * (height/width)

    pixels = {}
    xy_pixels = {}

    local function make_data(x, y)
        table.insert(pixels, { y_axis[y], x_axis[x], vector_fov})
        table.insert(xy_pixels, { x, height - y + 1})
    end

    for y = 1, height do
        for x = 1, width do
            make_data(x, y)
        end
    end
end

setGPUSize(16)

local function split(inputstr, sep)
    if sep == nil then sep = "%s" end
    local t={}
    for str in string.gmatch(inputstr, "([^"..sep.."]+)") do table.insert(t, str) end
    return t
end

local function clamp(num)
    return math.min(1, math.max(num, 0))
end

local mf = math.floor
local function shade_color(c, item)
    if not do_shade_color then return c end

    if item.distance == nil then item.distance=max_distance end
    local n = clamp(1 - (item.distance / max_distance) + shading_offset)

    return {mf(c[1]*n), mf(c[2]*n), mf(c[3]*n)}
end

local no_hit_color = {51, 102, 204}
local entity_color = {204, 76, 76}

local data = {
block = {
    minecraft={red_nether_brick_slab={69,7,9},potted_crimson_roots={127,8,41},oxidized_cut_copper_slab={79,153,126},potted_blue_orchid={47,162,168},polished_diorite={192,193,194},nether_portal={87,10,191},black_wall_banner={162,130,78},purple_wool={121,42,172},orange_bed={162,130,78},big_dripleaf={93,120,47},light_gray_concrete_powder={154,154,148},end_portal_frame={124,144,110},brain_coral={197,84,152},copper_ore={124,125,120},spawner={36,46,62},jungle_fence={160,115,80},bee_nest={187,146,78},creeper_wall_head={81,62,50},tripwire_hook={144,131,108},gray_stained_glass={76,76,76},redstone_ore={140,109,109},potted_fern={124,124,124},oxidized_copper={82,162,132},crimson_fence={101,48,70},deepslate_tile_wall={54,54,55},jungle_stairs={160,115,80},purpur_block={169,125,169},birch_log={204,197,172},cobblestone_slab={127,127,127},blue_candle={60,80,170},potted_warped_fungus={74,109,87},polished_granite_stairs={154,106,89},dark_oak_trapdoor={75,49,23},water_cauldron={82,82,84},brown_glazed_terracotta={119,106,85},diamond_ore={121,141,140},brown_terracotta={77,51,35},polished_diorite_slab={192,193,194},turtle_egg={218,215,178},acacia_stairs={168,90,50},cake={179,130,107},gray_candle={99,108,102},warped_roots={20,138,124},white_wall_banner={162,130,78},end_stone_brick_slab={218,224,162},white_terracotta={209,178,161},oak_door={139,109,64},chiseled_sandstone={220,208,162},magenta_wool={189,68,179},light_gray_candle={135,133,119},diorite_wall={188,188,188},sandstone={218,206,160},waxed_copper_block={192,107,79},candle_cake={210,169,139},fire_coral_wall_fan={158,34,45},fire={211,139,51},crafting_table={134,103,63},chorus_flower={123,91,123},jungle_sign={160,115,80},red_concrete_powder={168,54,50},red_mushroom_block={200,108,82},cyan_bed={162,130,78},cauldron={59,58,61},magenta_bed={162,130,78},snow={249,254,254},potatoes={74,131,44},warped_stem={55,84,93},yellow_stained_glass={229,229,51},twisting_vines_plant={20,135,122},orange_concrete={224,97,0},dark_prismarine_stairs={51,91,75},orange_stained_glass={216,127,51},prismarine_bricks={99,171,158},raw_gold_block={221,169,46},mossy_cobblestone_stairs={110,118,94},dandelion={147,172,43},cut_sandstone_slab={220,210,165},command_block={177,133,107},warped_wart_block={22,119,121},barrel={112,83,47},dark_oak_wood={60,46,26},beacon={102,148,152},stripped_oak_wood={177,144,86},cyan_candle={30,135,129},red_glazed_terracotta={181,59,53},enchanting_table={64,44,55},light_blue_wall_banner={162,130,78},warped_door={43,122,116},potted_oxeye_daisy={179,202,143},damaged_anvil={72,72,72},pumpkin_stem={0,0,0},white_stained_glass_pane={250,250,250},terracotta={152,94,67},polished_blackstone_slab={53,48,56},polished_blackstone={53,48,56},prismarine_brick_stairs={99,171,158},brown_candle={129,83,46},orange_glazed_terracotta={154,147,91},blue_glazed_terracotta={47,64,139},mossy_cobblestone={110,118,94},cut_red_sandstone={185,99,31},fletching_table={188,169,123},diorite_slab={188,188,188},basalt={76,77,82},spore_blossom={159,119,104},deepslate_emerald_ore={78,104,87},skeleton_wall_skull={81,62,50},brown_concrete={96,59,31},iron_bars={136,139,135},green_stained_glass={102,127,51},white_glazed_terracotta={188,212,202},potted_red_tulip={89,128,32},jungle_sapling={47,81,16},dark_oak_planks={66,43,20},green_terracotta={76,83,42},dark_oak_fence={66,43,20},white_tulip={93,164,71},observer={84,81,81},warped_fungus={74,109,87},prismarine_stairs={99,156,151},lime_shulker_box={99,172,23},dark_prismarine={51,91,75},light_gray_stained_glass_pane={150,150,150},lava_cauldron={89,65,52},purple_glazed_terracotta={109,48,152},dead_tube_coral_fan={128,122,118},skeleton_skull={81,62,50},nether_wart={1,1,1},vine={116,116,116},quartz_slab={235,229,222},crimson_slab={101,48,70},mossy_stone_brick_stairs={115,121,105},cyan_wall_banner={162,130,78},bedrock={85,85,85},candle={233,205,163},yellow_banner={162,130,78},campfire={136,107,60},red_shulker_box={140,31,30},cut_sandstone={220,210,165},magenta_concrete={169,48,159},cocoa={145,113,53},red_wool={160,39,34},oak_fence={162,130,78},warped_planks={43,104,99},brown_banner={162,130,78},blue_wall_banner={162,130,78},spruce_log={83,59,31},oak_sign={162,130,78},polished_blackstone_stairs={53,48,56},cornflower={79,121,146},snow_block={249,254,254},deepslate_brick_stairs={70,70,71},bubble_coral_wall_fan={160,32,159},dark_oak_pressure_plate={66,43,20},light_gray_banner={162,130,78},black_candle_cake={139,104,95},wet_sponge={171,181,70},dark_oak_sign={66,43,20},spruce_wood={58,37,16},birch_fence={192,175,121},jungle_slab={160,115,80},nether_gold_ore={115,54,42},melon={112,145,30},lily_of_the_valley={123,174,95},soul_fire={53,192,197},smooth_sandstone_stairs={223,214,170},birch_wood={216,215,210},cyan_shulker_box={20,121,135},dead_brain_coral={133,125,120},orange_candle={223,115,17},granite_wall={149,103,85},white_concrete={207,213,214},bricks={150,97,83},mossy_stone_brick_slab={115,121,105},polished_basalt={93,93,96},birch_slab={192,175,121},ender_chest={15,10,24},cobbled_deepslate_wall={77,77,80},pink_concrete_powder={228,153,181},tube_coral_wall_fan={50,91,208},waxed_oxidized_copper={82,162,132},nether_sprouts={19,151,133},hopper={63,63,65},acacia_planks={168,90,50},clay={160,166,179},cave_vines={97,111,41},birch_sign={192,175,121},orange_candle_cake={206,133,80},pink_candle_cake={202,133,132},item_frame={154,121,82},tube_coral={47,83,197},black_terracotta={37,22,16},dragon_wall_head={81,62,50},white_banner={162,130,78},amethyst_block={133,97,191},waxed_exposed_cut_copper_slab={154,121,101},warped_nylium={71,71,66},dead_fire_coral_wall_fan={124,118,114},crimson_roots={126,8,41},attached_melon_stem={77,77,77},redstone_block={3,3,3},orange_shulker_box={234,106,8},oak_trapdoor={124,99,56},birch_trapdoor={207,194,157},crying_obsidian={32,10,60},fern={124,124,124},soul_lantern={71,99,114},potted_birch_sapling={127,160,79},lilac={145,124,137},bamboo_sapling={92,89,35},spruce_planks={114,84,48},dead_bubble_coral={132,124,120},jungle_door={160,116,81},brick_slab={150,97,83},purpur_pillar={86,65,86},sculk_sensor={11,94,102},warped_wall_sign={43,104,99},deepslate_diamond_ore={83,106,106},polished_blackstone_wall={53,48,56},lime_carpet={112,185,25},cyan_terracotta={86,91,91},spruce_wall_sign={114,84,48},waxed_weathered_copper={108,153,110},birch_leaves={130,129,130},brown_mushroom={153,116,92},crimson_button={101,48,70},water={177,177,177},zombie_wall_head={81,62,50},melon_stem={153,153,153},light_blue_shulker_box={49,163,212},jack_o_lantern={202,128,33},chorus_plant={93,57,93},polished_deepslate={72,72,73},waxed_oxidized_cut_copper={79,153,126},prismarine_wall={99,156,151},acacia_sign={168,90,50},brown_shulker_box={106,66,35},gray_concrete_powder={76,81,84},chiseled_stone_bricks={119,118,119},blue_banner={162,130,78},purple_stained_glass_pane={124,62,174},deepslate_redstone_ore={104,73,74},piston={114,106,94},sponge={195,192,74},warped_sign={43,104,99},blue_wool={53,57,157},cut_copper_slab={191,106,80},potted_orange_tulip={93,142,30},birch_button={192,175,121},smooth_sandstone_slab={223,214,170},light_blue_candle_cake={137,147,153},light_gray_concrete={125,125,115},fire_coral_fan={158,34,45},warped_slab={43,104,99},black_bed={162,130,78},gray_candle_cake={156,130,114},polished_deepslate_stairs={72,72,73},spruce_slab={114,84,48},polished_andesite_stairs={132,134,133},white_wool={233,236,236},acacia_sapling={118,117,23},bone_block={219,216,193},coal_block={16,15,15},blue_bed={162,130,78},spruce_leaves={126,126,126},comparator={154,125,112},brain_coral_wall_fan={202,84,153},green_candle_cake={151,129,82},medium_amethyst_bud={158,120,201},purple_wall_banner={162,130,78},crimson_nylium={111,32,32},polished_blackstone_brick_slab={48,42,49},magenta_wall_banner={162,130,78},shulker_box={12,12,12},oxeye_daisy={179,202,143},light_blue_concrete={35,137,198},potted_crimson_fungus={141,44,29},horn_coral_block={216,199,66},cut_red_sandstone_slab={185,99,31},black_concrete_powder={25,26,31},dead_bubble_coral_wall_fan={140,134,130},lime_wall_banner={162,130,78},smooth_basalt={72,72,78},jungle_fence_gate={160,115,80},spruce_fence={114,84,48},target={227,173,162},orange_banner={162,130,78},weeping_vines={104,1,0},dark_oak_button={66,43,20},shroomlight={240,146,70},sand={219,207,163},mushroom_stem={202,183,152},chiseled_polished_blackstone={53,48,56},acacia_leaves={149,148,148},podzol={133,109,85},brown_stained_glass={102,76,51},infested_mossy_stone_bricks={115,121,105},potted_white_tulip={93,164,71},cracked_deepslate_tiles={52,52,52},smooth_quartz={236,230,223},redstone_lamp={119,78,45},rail={127,113,89},cyan_glazed_terracotta={52,118,125},potted_poppy={128,64,37},smooth_sandstone={223,214,170},crimson_planks={101,48,70},seagrass={50,126,8},polished_andesite_slab={132,134,133},rose_bush={114,74,37},stripped_spruce_log={110,85,49},beehive={166,133,79},yellow_shulker_box={248,188,29},stripped_spruce_wood={115,89,52},warped_hyphae={58,58,77},orange_carpet={240,118,19},hay_block={166,137,25},potted_bamboo={113,112,43},cyan_candle_cake={129,141,125},weathered_cut_copper_stairs={109,145,107},green_wool={84,109,27},mossy_cobblestone_wall={110,118,94},purple_banner={162,130,78},stripped_crimson_hyphae={137,57,90},chain={51,57,74},stripped_dark_oak_log={69,50,29},smooth_quartz_slab={236,230,223},stone_brick_wall={122,121,122},mossy_stone_bricks={115,121,105},iron_trapdoor={3,3,3},jungle_wall_sign={160,115,80},polished_granite={154,106,89},sandstone_wall={216,203,155},blackstone={42,35,41},dead_fire_coral_block={131,123,119},green_concrete={73,91,36},frosted_ice={138,180,252},daylight_detector={101,93,80},gray_bed={162,130,78},repeating_command_block={128,110,171},jungle_log={117,88,47},stripped_jungle_wood={171,132,84},potted_dead_bush={107,78,40},magenta_stained_glass={178,76,216},red_sandstone_slab={184,98,29},brown_wall_banner={162,130,78},light_blue_wool={58,175,217},powered_rail={146,109,74},lightning_rod={225,183,169},pink_candle={212,116,146},polished_deepslate_slab={72,72,73},dead_horn_coral_block={133,126,122},red_concrete={142,32,32},wither_skeleton_wall_skull={81,62,50},dark_oak_sapling={61,90,30},obsidian={15,10,24},fire_coral={166,37,46},dead_tube_coral={118,111,107},spruce_fence_gate={114,84,48},brown_carpet={114,71,40},bubble_coral={161,23,159},black_stained_glass_pane={24,24,24},white_candle={213,217,211},magenta_carpet={189,68,179},stripped_oak_log={168,136,81},yellow_concrete_powder={232,199,54},black_shulker_box={25,25,29},iron_ore={136,129,122},lodestone={133,134,137},twisting_vines={20,143,124},red_stained_glass_pane={74,25,25},yellow_wool={248,197,39},cobweb={228,233,234},green_wall_banner={162,130,78},exposed_cut_copper={154,121,101},bubble_coral_fan={160,32,159},blue_concrete={44,46,143},granite_stairs={149,103,85},black_stained_glass={25,25,25},pink_stained_glass={242,127,165},dead_tube_coral_wall_fan={128,122,118},iron_door={194,193,193},pink_tulip={99,157,78},wither_rose={41,44,23},chiseled_deepslate={54,54,54},andesite={136,136,136},raw_iron_block={166,135,107},deepslate_coal_ore={74,74,76},gilded_blackstone={55,42,38},yellow_bed={162,130,78},polished_blackstone_brick_wall={48,42,49},infested_cobblestone={127,127,127},dark_oak_slab={66,43,20},tube_coral_block={49,87,206},polished_diorite_stairs={192,193,194},glowstone={171,131,84},exposed_copper={161,125,103},brown_mushroom_block={175,140,100},stone_brick_stairs={122,121,122},stripped_warped_stem={29,76,74},soul_campfire={76,128,126},pumpkin={196,116,24},anvil={72,72,72},iron_block={220,220,220},cobbled_deepslate_stairs={77,77,80},white_concrete_powder={225,227,227},yellow_concrete={240,175,21},composter={117,79,39},polished_andesite={132,134,133},activator_rail={129,87,74},stripped_birch_wood={196,176,118},furnace={110,108,103},gray_wall_banner={162,130,78},acacia_slab={168,90,50},stone_button={125,125,125},glow_item_frame={170,139,92},end_stone={219,222,158},player_head={81,62,50},spruce_trapdoor={103,79,47},orange_concrete_powder={227,131,31},oak_button={162,130,78},granite_slab={149,103,85},lime_candle={111,176,26},dried_kelp_block={46,55,35},sticky_piston={106,112,95},red_mushroom={216,75,67},potted_lily_of_the_valley={123,174,95},smithing_table={58,39,42},black_candle={56,42,54},tall_seagrass={52,128,9},mossy_cobblestone_slab={110,118,94},dark_oak_wall_sign={66,43,20},diorite_stairs={188,188,188},brown_bed={162,130,78},green_bed={162,130,78},blackstone_wall={42,35,40},sea_pickle={90,97,39},lime_candle_cake={161,158,84},birch_stairs={192,175,121},stripped_dark_oak_wood={72,56,36},deepslate={83,83,86},sandstone_stairs={218,206,160},stripped_warped_hyphae={57,150,147},dead_fire_coral={136,128,124},yellow_candle={212,174,62},amethyst_cluster={163,126,207},chiseled_red_sandstone={182,97,29},redstone_wire={180,180,180},light_blue_glazed_terracotta={94,164,208},smooth_quartz_stairs={236,230,223},dead_horn_coral_wall_fan={134,125,121},waxed_weathered_cut_copper_slab={109,145,107},white_carpet={233,236,236},red_sandstone={184,98,29},cracked_stone_bricks={118,117,118},gold_ore={145,133,106},lime_concrete_powder={125,189,41},trapped_chest={162,130,78},magma_block={142,63,31},crimson_trapdoor={103,50,72},infested_cracked_stone_bricks={118,117,118},honeycomb_block={229,148,29},oxidized_cut_copper_stairs={79,153,126},smooth_stone={158,158,158},attached_pumpkin_stem={69,69,69},nether_brick_stairs={3,3,3},dark_oak_fence_gate={66,43,20},brown_wool={114,71,40},end_portal={15,10,24},light_blue_stained_glass_pane={99,150,212},lime_glazed_terracotta={162,197,55},pink_wool={237,141,172},crimson_wall_sign={101,48,70},brewing_stand={119,103,93},light_blue_candle={52,149,199},purple_bed={162,130,78},hanging_roots={161,115,91},infested_chiseled_stone_bricks={119,118,119},wither_skeleton_skull={81,62,50},oak_fence_gate={162,130,78},pointed_dripstone={132,106,91},stripped_birch_log={194,173,117},stripped_crimson_stem={69,29,46},powder_snow_cauldron={97,97,100},dead_bush={107,78,40},stone={125,125,125},acacia_fence_gate={168,90,50},honey_block={247,173,42},horn_coral_wall_fan={205,183,61},cobblestone={127,127,127},potted_acacia_sapling={118,117,23},glass_pane={173,212,218},ice={145,183,253},allium={158,137,183},crimson_fungus={141,44,29},infested_stone_bricks={122,121,122},dispenser={83,83,83},powder_snow={248,253,253},beetroots={73,124,38},white_candle_cake={202,174,158},sea_lantern={172,199,190},lime_terracotta={103,117,52},gray_shulker_box={55,58,62},bubble_column={177,177,177},emerald_ore={108,136,115},cracked_polished_blackstone_bricks={44,37,43},bamboo={79,125,23},cracked_nether_bricks={40,20,23},oak_sapling={77,106,40},dripstone_block={134,107,92},soul_sand={81,62,50},conduit={159,139,113},horn_coral={209,186,62},ancient_debris={95,65,57},smoker={100,90,77},red_nether_bricks={69,7,9},magenta_stained_glass_pane={174,74,212},spruce_stairs={114,84,48},blue_carpet={53,57,157},bell={126,105,62},weathered_cut_copper={109,145,107},red_terracotta={143,61,46},loom={128,99,70},azalea={97,121,46},deepslate_gold_ore={115,102,78},pink_shulker_box={230,121,157},oak_log={130,103,61},stripped_acacia_wood={174,92,59},green_stained_glass_pane={99,124,49},jungle_trapdoor={152,110,77},potted_red_mushroom={216,75,67},birch_door={214,201,160},orange_tulip={93,142,30},lava={212,90,18},red_stained_glass={1,1,1},yellow_candle_cake={202,157,98},lever={119,110,97},tnt={163,72,63},oak_planks={162,130,78},tinted_glass={43,38,45},light_gray_carpet={142,142,134},azure_bluet={169,204,127},yellow_wall_banner={162,130,78},blackstone_stairs={42,35,41},cut_copper={191,106,80},yellow_stained_glass_pane={225,225,49},jukebox={49,34,25},cut_copper_stairs={191,106,80},red_banner={162,130,78},light_gray_glazed_terracotta={144,166,167},blast_furnace={102,101,100},small_dripleaf={97,121,46},pink_carpet={237,141,172},dark_oak_leaves={150,150,150},respawn_anchor={45,24,73},cave_vines_plant={96,104,39},bookshelf={139,112,69},redstone_torch={136,74,43},sugar_cane={148,192,101},waxed_oxidized_cut_copper_slab={79,153,126},red_sandstone_wall={186,99,29},heavy_weighted_pressure_plate={220,220,220},soul_torch={109,115,89},dirt={134,96,67},white_stained_glass={255,255,255},blue_ice={116,167,253},diamond_block={98,237,228},light_gray_bed={162,130,78},stone_stairs={125,125,125},smooth_stone_slab={163,163,163},smooth_red_sandstone_slab={181,97,31},red_nether_brick_wall={69,7,9},rooted_dirt={144,103,76},blue_orchid={47,162,168},nether_brick_fence={3,3,3},potted_azure_bluet={169,204,127},chiseled_quartz_block={231,226,217},azalea_leaves={90,114,44},brick_wall={150,97,83},kelp_plant={86,130,42},pink_concrete={213,101,142},sandstone_slab={218,206,160},waxed_exposed_cut_copper_stairs={154,121,101},copper_block={192,107,79},granite={149,103,85},light_weighted_pressure_plate={246,208,61},moss_block={89,109,45},crimson_door={114,55,79},magenta_candle={171,57,157},chiseled_nether_bricks={47,23,28},warped_fence_gate={43,104,99},andesite_slab={136,136,136},nether_brick_wall={3,3,3},lime_stained_glass_pane={124,200,24},cyan_wool={21,137,145},green_candle={86,105,21},dead_fire_coral_fan={124,118,114},dark_oak_door={75,50,24},birch_pressure_plate={192,175,121},sweet_berry_bush={54,87,55},cyan_carpet={21,137,145},prismarine_brick_slab={99,171,158},birch_fence_gate={192,175,121},polished_blackstone_brick_stairs={48,42,49},purple_concrete_powder={131,55,177},yellow_carpet={248,197,39},deepslate_iron_ore={106,99,94},jungle_planks={160,115,80},brain_coral_block={207,91,159},red_nether_brick_stairs={69,7,9},petrified_oak_slab={162,130,78},piston_head={129,127,92},green_banner={162,130,78},cartography_table={78,57,36},red_candle_cake={183,108,90},dead_brain_coral_block={124,117,114},cyan_banner={162,130,78},nether_brick_slab={3,3,3},blue_stained_glass_pane={25,37,86},polished_deepslate_wall={72,72,73},cyan_concrete_powder={36,147,157},oak_wall_sign={162,130,78},gold_block={246,208,61},gray_glazed_terracotta={83,90,93},white_shulker_box={215,220,221},green_concrete_powder={97,119,44},potted_oak_sapling={77,106,40},tall_grass={139,138,139},flowering_azalea={111,117,68},deepslate_tile_stairs={54,54,55},exposed_cut_copper_slab={154,121,101},detector_rail={130,104,89},crimson_pressure_plate={101,48,70},stripped_jungle_log={168,127,83},lectern={155,121,71},fire_coral_block={163,35,46},orange_wall_banner={162,130,78},potted_wither_rose={41,44,23},coarse_dirt={119,85,59},infested_deepslate={83,83,86},gray_carpet={62,68,71},dead_tube_coral_block={130,123,119},deepslate_lapis_ore={79,90,115},stone_bricks={122,121,122},cobblestone_stairs={127,127,127},red_candle={166,52,41},prismarine={99,156,151},waxed_exposed_cut_copper={154,121,101},deepslate_bricks={70,70,71},smooth_red_sandstone={181,97,31},gray_banner={162,130,78},purple_shulker_box={103,32,156},brown_candle_cake={168,120,92},purpur_stairs={169,125,169},cobbled_deepslate={77,77,80},light_blue_terracotta={113,108,137},lime_bed={162,130,78},pink_wall_banner={162,130,78},green_carpet={84,109,27},large_amethyst_bud={161,126,202},black_concrete={8,10,15},calcite={223,224,220},jungle_pressure_plate={160,115,80},weathered_copper={108,153,110},grass_block={146,131,113},potted_jungle_sapling={47,81,16},light_gray_wool={142,142,134},stripped_acacia_log={170,91,55},polished_blackstone_bricks={48,42,49},pink_terracotta={161,78,78},blue_stained_glass={1,1,1},blue_candle_cake={141,119,141},jungle_leaves={156,154,143},warped_button={43,104,99},dark_prismarine_slab={51,91,75},brick_stairs={150,97,83},andesite_wall={136,136,136},light_gray_candle_cake={171,140,121},jungle_wood={85,67,25},creeper_head={81,62,50},black_banner={162,130,78},yellow_terracotta={186,133,35},soul_soil={75,57,46},quartz_bricks={234,229,221},crimson_sign={101,48,70},netherrack={97,38,38},deepslate_brick_slab={70,70,71},magenta_concrete_powder={192,83,184},gray_wool={62,68,71},crimson_hyphae={92,25,29},warped_trapdoor={47,119,111},weathered_cut_copper_slab={109,145,107},oak_pressure_plate={162,130,78},spruce_sapling={44,60,36},acacia_wall_sign={168,90,50},crimson_stem={102,37,49},poppy={128,64,37},redstone_wall_torch={136,74,43},andesite_stairs={136,136,136},potted_allium={158,137,183},end_rod={205,196,185},purpur_slab={169,125,169},stone_brick_slab={122,121,122},spruce_button={114,84,48},green_glazed_terracotta={117,142,67},magenta_candle_cake={185,110,136},mossy_stone_brick_wall={115,121,105},cracked_deepslate_bricks={64,64,65},spruce_pressure_plate={114,84,48},warped_pressure_plate={43,104,99},nether_wart_block={114,2,2},scaffolding={196,164,92},grass={145,145,145},purple_candle_cake={163,100,134},horn_coral_fan={205,183,61},polished_blackstone_pressure_plate={53,48,56},dark_oak_stairs={66,43,20},peony={108,113,116},nether_bricks={3,3,3},quartz_pillar={235,230,223},quartz_stairs={235,229,222},end_gateway={15,10,24},glow_lichen={112,130,121},orange_wool={240,118,19},warped_fence={43,104,99},orange_terracotta={161,83,37},waxed_weathered_cut_copper={109,145,107},ladder={124,96,54},light_gray_terracotta={135,106,97},black_glazed_terracotta={67,30,32},structure_block={75,63,77},deepslate_copper_ore={92,93,89},stone_pressure_plate={125,125,125},yellow_glazed_terracotta={234,192,88},quartz_block={235,229,222},budding_amethyst={132,96,186},torch={138,113,63},soul_wall_torch={109,115,89},light_blue_carpet={58,175,217},lapis_block={30,67,140},dropper={112,112,112},exposed_cut_copper_stairs={154,121,101},lantern={106,91,83},farmland={119,81,51},red_sandstone_stairs={184,98,29},carrots={58,117,38},stonecutter={142,137,132},oak_stairs={162,130,78},slime_block={2,2,2},prismarine_slab={99,156,151},potted_flowering_azalea_bush={112,116,69},warped_stairs={43,104,99},dead_brain_coral_fan={132,125,121},waxed_cut_copper_stairs={191,106,80},potted_cactus={99,108,47},potted_azalea_bush={98,120,47},infested_stone={125,125,125},brain_coral_fan={202,84,153},red_tulip={89,128,32},acacia_log={127,92,71},player_wall_head={81,62,50},flowering_azalea_leaves={99,111,60},light_gray_stained_glass={153,153,153},wheat={21,20,10},light_blue_banner={162,130,78},dead_bubble_coral_fan={140,134,130},blue_concrete_powder={70,73,166},black_carpet={20,21,25},chipped_anvil={72,72,72},wall_torch={138,113,63},red_bed={162,130,78},acacia_wood={103,96,86},spruce_sign={114,84,48},tube_coral_fan={50,91,208},mycelium={113,97,84},bubble_coral_block={165,26,162},dirt_path={139,106,66},light_blue_bed={162,130,78},waxed_cut_copper_slab={191,106,80},deepslate_tiles={54,54,55},polished_granite_slab={154,106,89},weeping_vines_plant={132,16,12},dead_horn_coral={142,135,129},light_blue_stained_glass={102,153,216},birch_planks={192,175,121},crimson_fence_gate={101,48,70},potted_dark_oak_sapling={61,90,30},pink_bed={162,130,78},cobblestone_wall={127,127,127},black_wool={20,21,25},lapis_ore={107,117,141},birch_sapling={127,160,79},dead_brain_coral_wall_fan={132,125,121},smooth_red_sandstone_stairs={181,97,31},green_shulker_box={79,100,31},birch_wall_sign={192,175,121},glass={175,213,219},polished_blackstone_button={53,48,56},tripwire={129,129,129},deepslate_brick_wall={70,70,71},waxed_cut_copper={191,106,80},potted_cornflower={79,121,146},purple_candle={115,34,152},moving_piston={110,104,96},repeater={141,117,107},white_bed={162,130,78},brown_concrete_powder={125,84,53},lime_wool={112,185,25},dragon_head={81,62,50},tuff={108,109,102},purple_concrete={100,31,156},light_gray_shulker_box={124,124,115},lime_concrete={94,168,24},dark_oak_log={64,45,24},potted_warped_roots={20,136,123},end_stone_brick_wall={218,224,162},magenta_shulker_box={173,54,163},lily_pad={133,133,133},coal_ore={105,105,105},gray_stained_glass_pane={74,74,74},moss_carpet={89,109,45},cactus={105,142,58},diorite={188,188,188},red_wall_banner={162,130,78},jigsaw={55,47,57},blue_terracotta={74,59,91},red_carpet={160,39,34},flower_pot={129,82,60},magenta_terracotta={149,88,108},lime_banner={162,130,78},oak_slab={162,130,78},pink_glazed_terracotta={235,154,181},emerald_block={42,203,87},cyan_stained_glass_pane={74,124,150},brown_stained_glass_pane={99,74,49},waxed_oxidized_cut_copper_stairs={79,153,126},potted_brown_mushroom={153,116,92},purple_stained_glass={127,63,178},orange_stained_glass_pane={212,124,49},nether_quartz_ore={117,65,62},end_stone_brick_stairs={218,224,162},light_blue_concrete_powder={74,180,213},pink_stained_glass_pane={237,124,162},kelp={87,140,44},red_sand={190,102,33},acacia_fence={168,90,50},potted_dandelion={147,172,43},acacia_pressure_plate={168,90,50},oxidized_cut_copper={79,153,126},pink_banner={162,130,78},grindstone={103,93,82},gray_terracotta={57,42,35},lime_stained_glass={127,204,25},gray_concrete={54,57,61},dead_bubble_coral_block={131,123,119},acacia_trapdoor={156,87,51},end_stone_bricks={218,224,162},chain_command_block={130,161,147},chest={162,130,78},sunflower={101,147,36},oak_wood={109,85,50},large_fern={66,66,66},light_gray_wall_banner={162,130,78},gravel={131,127,126},cobbled_deepslate_slab={77,77,80},jungle_button={160,115,80},magenta_glazed_terracotta={208,100,191},small_amethyst_bud={131,99,192},crimson_stairs={101,48,70},big_dripleaf_stem={91,115,45},blackstone_slab={42,35,41},potted_spruce_sapling={44,60,36},cyan_concrete={21,119,136},deepslate_tile_slab={54,54,55},cyan_stained_glass={76,127,153},netherite_block={66,61,63},waxed_exposed_copper={161,125,103},waxed_weathered_cut_copper_stairs={109,145,107},blue_shulker_box={43,45,140},purple_carpet={121,42,172},dragon_egg={12,9,15},magenta_banner={162,130,78},note_block={88,58,40},stone_slab={125,125,125},packed_ice={141,180,250},zombie_head={81,62,50},potted_pink_tulip={99,157,78},oak_leaves={144,144,144},raw_copper_block={154,105,79},acacia_button={168,90,50},purple_terracotta={118,70,86},acacia_door={165,93,59},spruce_door={106,80,48},carved_pumpkin={181,105,21},dead_horn_coral_fan={134,125,121}}
    }
}

local function get_color_normal(item)
    if item.is_block then
        local res = data
        for _, key in ipairs(split(item.block_type, ".")) do
            res = res[key]
            if res == nil then return shade_color(no_hit_color, item) end
        end
        if res == nil then return shade_color(no_hit_color, item) end
        return shade_color(res, item)
    end

    return shade_color(entity_color, item)
end

local function get_color_depthmap(item)
    if (item.distance == nil) then return 0 end
    local num = (1 - item.distance / max_distance) * 255
    return {math.floor(num), math.floor(num), math.floor(num)}
end

local dt
for k, v in pairs(port.getConnected()) do
    dt = v
end

local start = os.epoch("utc")
local i = 0
local function count()
    if (os.epoch("utc") - start > 1000) then
        print("pixels " .. i)
        i = 0
        start = os.epoch("utc")
    end
    i = i + 1
end

local function write_pixels(at, qdata)
    local img = get_current_image(not cur_img)
    while at <= #qdata.results do
        local item = qdata.results[at]
        local r, g, b = table.unpack(depth_map and get_color_depthmap(item) or get_color_normal(item))

        local x, y = xy_pixels[at][1], xy_pixels[at][2]

        img.setRGB(x-2, y-2, r, g, b)

        at = at + 1

        count()
    end

    return at
end

local yield_point = os.epoch("utc")
local function yield() os.queueEvent("yield") os.pullEvent("yield")  end
local function try_yield() if os.epoch("utc") - yield_point >= 100 then yield() yield_point = os.epoch("utc") end end

local qdata
local draw_image = false
local did_draw_image = false

local pause = false
local paused = false

local function write_data()
    dt.queueRaycasts(max_distance, pixels, euler_mode, do_cache)
    local at = 1
    qdata = dt.getQueuedData()
    while not qdata.is_done do
        qdata.results = qdata.results or {}
        at = write_pixels(at, qdata)
        qdata = dt.getQueuedData()
        try_yield()
    end
    write_pixels(at, qdata)
end

local function raycasting_thread()
    while true do
        draw_image = false
        max_distance = new_distance

        write_data()

        toggle_current_image()
        draw_image = true

        while not did_draw_image do
            if pause then
                paused = true
                while pause do
                    yield()
                end
                paused = false
            end
            yield()
        end
        did_draw_image = false
    end
end

local function drawing_thread()
    local fps = 0
    local point = os.epoch("utc")
    local last_update = point
    while true do
        if draw_image then
            gpu.drawImage(1, 1, get_current_image().ref())
            gpu.sync()

            fps = fps + 1

            last_update = os.epoch("utc")
            did_draw_image = true
            if os.epoch("utc") - point > 1000 then
                print("FPS "..fps)
                fps = 0
                point = os.epoch("utc")
            end
        end
        if show_partial and os.epoch("utc") - last_update > 1000 then
            gpu.drawImage(1, 1, get_current_image().ref())
            gpu.sync()
        end
        sleep()
    end
end

local function message_events_thread()
    if chat == nil then return end
    while true do
        local event, username, message, uuid, isHidden = os.pullEvent("chat")
        local num = tonumber(message)
        if num ~= nil then
            new_distance = num
            dt.terminateAll()
            qdata.is_done = true
        end
        if message == "shade" then
            if do_shade_color then do_shade_color = false else do_shade_color = true end
        end
        if string.sub(message, 1, 5) == "size " then
            local num = tonumber(string.sub(message, 6))
            if num ~= nil and num >= 16 and num <= 64 then
                pause = true
                qdata.is_done = true

                while not paused do
                    yield()
                end

                setGPUSize(num)
                pause = false
            end
        end

        if string.sub(message, 1, 6) == "shade " then
            local num = tonumber(string.sub(message, 7))
            if num ~= nil and num >= 0 and num <= 1 then
                shading_offset = num
            end
        end
        if message == "depthmap" then
            depth_map = not depth_map
        end
        if message == "show_partial" then
            show_partial = not show_partial
        end
    end
end

parallel.waitForAll(
         raycasting_thread
        ,drawing_thread
        ,message_events_thread
)