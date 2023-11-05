local rc = peripheral.find("raycaster")
--local monitor = peripheral.wrap('left')
 local monitor = peripheral.find("monitor")

local euler_mode = false
local depth_map = false
local cache = false

local width_yaw_range = math.rad(45)
local height_pitch_range = math.rad(45)
local max_distance = 20
local vector_fov = 1

local width, height = monitor.getSize()
height_pitch_range = height_pitch_range * (height/width)

local char_data = {{{"\0",false},{"\9",false},{"\10",false},{"\13",false},{"\32",false},{"\128",false},{"\160",false},},{{"\46",false},{"\168",false},{"\180",false},{"\184",false},},{{"\39",false},{"\44",false},{"\96",false},},{{"\58",false},{"\183",false},},{{"\45",false},{"\59",false},{"\94",false},{"\95",false},{"\173",false},{"\175",false},},{{"\33",false},{"\34",false},{"\42",false},{"\105",false},{"\126",false},{"\161",false},{"\166",false},},{{"\28",false},{"\47",false},{"\60",false},{"\62",false},{"\92",false},{"\108",false},{"\124",false},{"\172",false},{"\178",false},{"\236",false},{"\237",false},{"\239",false},{"\247",false},},{{"\22",false},{"\176",false},{"\179",false},{"\185",false},{"\238",false},},{{"\40",false},{"\41",false},{"\43",false},{"\63",false},{"\89",false},{"\114",false},{"\116",false},{"\118",false},{"\120",false},{"\123",false},{"\125",false},{"\129",false},{"\130",false},{"\132",false},{"\136",false},{"\144",false},{"\159",true},{"\191",false},{"\215",false},{"\221",false},},{{"\29",false},{"\61",false},{"\74",false},{"\171",false},{"\187",false},},{{"\26",false},{"\27",false},{"\37",false},{"\73",false},{"\76",false},{"\84",false},{"\91",false},{"\93",false},{"\99",false},{"\102",false},{"\106",false},{"\170",false},{"\204",false},{"\205",false},{"\207",false},},{{"\7",false},{"\19",false},{"\49",false},{"\55",false},{"\107",false},{"\110",false},{"\111",false},{"\117",false},{"\186",false},{"\206",false},{"\219",false},},{{"\4",false},{"\16",false},{"\17",false},{"\24",false},{"\25",false},{"\30",false},{"\31",false},{"\67",false},{"\70",false},{"\86",false},{"\88",false},{"\109",false},{"\115",false},{"\122",false},{"\162",false},{"\189",false},{"\217",false},{"\218",false},{"\220",false},{"\231",false},{"\251",false},{"\254",false},},{{"\11",false},{"\51",false},{"\80",false},{"\97",false},{"\104",false},{"\112",false},{"\113",false},{"\119",false},{"\177",false},{"\188",false},{"\199",false},{"\222",false},{"\242",false},{"\243",false},{"\246",false},{"\249",false},{"\250",false},{"\252",false},},{{"\14",false},{"\36",false},{"\38",false},{"\52",false},{"\54",false},{"\57",false},{"\75",false},{"\83",false},{"\85",false},{"\90",false},{"\101",false},{"\121",false},{"\181",false},{"\197",false},{"\229",false},{"\240",false},{"\248",false},},{{"\50",false},{"\79",false},{"\81",false},{"\98",false},{"\100",false},{"\163",false},{"\164",false},{"\192",false},{"\193",false},{"\196",false},{"\210",false},{"\211",false},{"\213",false},{"\214",false},{"\224",false},{"\225",false},{"\228",false},{"\241",false},{"\245",false},},{{"\12",false},{"\53",false},{"\56",false},{"\69",false},{"\71",false},{"\72",false},{"\77",false},{"\78",false},{"\87",false},{"\103",false},{"\165",false},{"\190",false},{"\209",false},{"\212",false},{"\230",false},{"\232",false},{"\233",false},{"\235",false},{"\244",false},{"\253",false},{"\255",false},},{{"\6",false},{"\8",true},{"\65",false},{"\68",false},{"\82",false},{"\127",false},{"\131",false},{"\133",false},{"\134",false},{"\137",false},{"\138",false},{"\140",false},{"\143",true},{"\145",false},{"\146",false},{"\148",false},{"\151",true},{"\152",false},{"\155",true},{"\157",true},{"\158",true},{"\195",false},{"\200",false},{"\201",false},{"\203",false},{"\227",false},},{{"\1",false},{"\5",false},{"\20",false},{"\48",false},{"\182",false},{"\194",false},{"\208",false},{"\216",false},{"\223",false},{"\226",false},},{{"\18",false},{"\21",false},{"\35",false},{"\66",false},{"\167",false},{"\169",false},{"\198",false},{"\234",false},},{{"\3",false},{"\202",false},},{{"\15",false},{"\174",false},},{{"\23",false},{"\64",false},},{{"\2",false},},{{"\135",false},{"\135",true},{"\139",false},{"\139",true},{"\141",false},{"\141",true},{"\142",false},{"\142",true},{"\147",false},{"\147",true},{"\149",false},{"\149",true},{"\150",false},{"\150",true},{"\153",false},{"\153",true},{"\154",false},{"\154",true},{"\156",false},{"\156",true},},{{"\2",true},},{{"\23",true},{"\64",true},},{{"\15",true},{"\174",true},},{{"\3",true},{"\202",true},},{{"\18",true},{"\21",true},{"\35",true},{"\66",true},{"\167",true},{"\169",true},{"\198",true},{"\234",true},},{{"\1",true},{"\5",true},{"\20",true},{"\48",true},{"\182",true},{"\194",true},{"\208",true},{"\216",true},{"\223",true},{"\226",true},},{{"\6",true},{"\8",false},{"\65",true},{"\68",true},{"\82",true},{"\127",true},{"\131",true},{"\133",true},{"\134",true},{"\137",true},{"\138",true},{"\140",true},{"\143",false},{"\145",true},{"\146",true},{"\148",true},{"\151",false},{"\152",true},{"\155",false},{"\157",false},{"\158",false},{"\195",true},{"\200",true},{"\201",true},{"\203",true},{"\227",true},},{{"\12",true},{"\53",true},{"\56",true},{"\69",true},{"\71",true},{"\72",true},{"\77",true},{"\78",true},{"\87",true},{"\103",true},{"\165",true},{"\190",true},{"\209",true},{"\212",true},{"\230",true},{"\232",true},{"\233",true},{"\235",true},{"\244",true},{"\253",true},{"\255",true},},{{"\50",true},{"\79",true},{"\81",true},{"\98",true},{"\100",true},{"\163",true},{"\164",true},{"\192",true},{"\193",true},{"\196",true},{"\210",true},{"\211",true},{"\213",true},{"\214",true},{"\224",true},{"\225",true},{"\228",true},{"\241",true},{"\245",true},},{{"\14",true},{"\36",true},{"\38",true},{"\52",true},{"\54",true},{"\57",true},{"\75",true},{"\83",true},{"\85",true},{"\90",true},{"\101",true},{"\121",true},{"\181",true},{"\197",true},{"\229",true},{"\240",true},{"\248",true},},{{"\11",true},{"\51",true},{"\80",true},{"\97",true},{"\104",true},{"\112",true},{"\113",true},{"\119",true},{"\177",true},{"\188",true},{"\199",true},{"\222",true},{"\242",true},{"\243",true},{"\246",true},{"\249",true},{"\250",true},{"\252",true},},{{"\4",true},{"\16",true},{"\17",true},{"\24",true},{"\25",true},{"\30",true},{"\31",true},{"\67",true},{"\70",true},{"\86",true},{"\88",true},{"\109",true},{"\115",true},{"\122",true},{"\162",true},{"\189",true},{"\217",true},{"\218",true},{"\220",true},{"\231",true},{"\251",true},{"\254",true},},{{"\7",true},{"\19",true},{"\49",true},{"\55",true},{"\107",true},{"\110",true},{"\111",true},{"\117",true},{"\186",true},{"\206",true},{"\219",true},},{{"\26",true},{"\27",true},{"\37",true},{"\73",true},{"\76",true},{"\84",true},{"\91",true},{"\93",true},{"\99",true},{"\102",true},{"\106",true},{"\170",true},{"\204",true},{"\205",true},{"\207",true},},{{"\29",true},{"\61",true},{"\74",true},{"\171",true},{"\187",true},},{{"\40",true},{"\41",true},{"\43",true},{"\63",true},{"\89",true},{"\114",true},{"\116",true},{"\118",true},{"\120",true},{"\123",true},{"\125",true},{"\129",true},{"\130",true},{"\132",true},{"\136",true},{"\144",true},{"\159",false},{"\191",true},{"\215",true},{"\221",true},},{{"\22",true},{"\176",true},{"\179",true},{"\185",true},{"\238",true},},{{"\28",true},{"\47",true},{"\60",true},{"\62",true},{"\92",true},{"\108",true},{"\124",true},{"\172",true},{"\178",true},{"\236",true},{"\237",true},{"\239",true},{"\247",true},},{{"\33",true},{"\34",true},{"\42",true},{"\105",true},{"\126",true},{"\161",true},{"\166",true},},{{"\45",true},{"\59",true},{"\94",true},{"\95",true},{"\173",true},{"\175",true},},{{"\58",true},{"\183",true},},{{"\39",true},{"\44",true},{"\96",true},},{{"\46",true},{"\168",true},{"\180",true},{"\184",true},},{{"\0",true},{"\9",true},{"\10",true},{"\13",true},{"\32",true},{"\128",true},{"\160",true},},}

local colors = {}
for i = 0, 15 do
    colors[i] = string.format("%x", i)
end

monitor.setTextScale(0.5)
monitor.setCursorPos(1, 1)
monitor.clear()
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

local function split(inputstr, sep)
    if sep == nil then sep = "%s" end
    local t={}
    for str in string.gmatch(inputstr, "([^"..sep.."]+)") do table.insert(t, str) end
    return t
end

if depth_map then
    for i, num in ipairs(linspace(1, 0, 16)) do
        monitor.setPaletteColor(2^(i-1), num, num, num)
    end
end

local data = {
block = {
    minecraft = {
        gold_block = "4",

        polished_diorite = "8",
        stone = "8",
        iron_ore = "8",
        gravel = "8",
        coal_ore = "8",
        emerald_ore = "8",

        waxed_cut_copper_slab = "1",
        spruce_door = "1",
        dirt = "1",

        oak_planks = "c",
        lectern = "c",
        oak_log = "c",
        acacia_log = "c",

        grass_block = "d",
        grass = "d",
        tall_grass = "d",

        oak_leaves = "5",
        acacia_leaves = "5",

        crimson_planks = "a",
        }
    }
}

local function get_pixel_normal(item)
    if item.is_block then
        local res = data
        for _, key in ipairs(split(item.block_type, ".")) do
            res = res[key]
            if res == nil then return {" ", "0", "b"} end
        end
        if res == nil then return {" ", "0", "b"} end
        return {" ", "0", res}
    end

    return {" ", "0", "e"}
end

local function get_pixel_depthmap(item)
    if (item.distance == nil or item.distance > max_distance) then return {" ", "0", colors[15]} end
    local rel = (item.distance / max_distance) * 15
    local frel = math.floor(rel)

    local rel_chr_line = char_data[math.floor((1 - rel + frel) * (#char_data-1))+1]
    local rel_chr = rel_chr_line[math.floor(math.random()*(#rel_chr_line-1))+1]

    local fcolor = colors[frel]
    local scolor = colors[math.min(15, frel+1)]

    return {
        rel_chr[1],
        rel_chr[2] and scolor or fcolor,
        rel_chr[2] and fcolor or scolor
    }
end

local start = os.epoch("utc")
local i = 0
local function yield()
    if (os.epoch("utc") - start > 1000) then
        print("pixels " .. i)
        i = 0
        os.queueEvent("yield")
        os.pullEvent("yield")
        start = os.epoch("utc")
    end
    i = i + 1
end


local x_axis = linspace(-width_yaw_range, width_yaw_range, width)
local y_axis = linspace(-height_pitch_range, height_pitch_range, height)

monitor.setCursorPos(1, height)
for y=1, height do
for x=1, width do
    yield()

    monitor.setCursorPos(x, height - y + 1)

    local yr = y_axis[y]
    local xr = x_axis[x]

    local item = rc.raycast(max_distance, {yr, xr, vector_fov}, euler_mode, true, cache)
    local pix = depth_map and get_pixel_depthmap(item) or get_pixel_normal(item)
    monitor.blit(pix[1], pix[2], pix[3])
end end