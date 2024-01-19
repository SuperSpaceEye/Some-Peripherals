-- Uses monitor from Tom's Peripherals as output https://modrinth.com/mod/toms-peripherals
local rc = peripheral.find("raycaster")
local gpu = peripheral.find("tm_gpu")

local euler_mode = false
local depth_map = false

local width_roll_range = math.rad(45)
local height_pitch_range = math.rad(45)
local max_distance = 20
local vector_fov = 1

gpu.refreshSize()
gpu.fill()
gpu.sync()

gpu.setSize(32)
local width, height = gpu.getSize()
local img = gpu.newImage(width, height)

height_pitch_range = height_pitch_range * (height/width)

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

local function fromBlit(hex)
    if #hex ~= 1 then return nil end
    local value = tonumber(hex, 16)
    if not value then return nil end

    return 2 ^ value
end

local function clamp(num)
    return math.min(1, math.max(num, 0))
end

local function fromBlitChar(chr, item)
    if (item.distance == nil) then item.distance=max_distance end
    local n = clamp(1 - (item.distance / max_distance) + 0.2)

    local r, g, b = term.nativePaletteColor(fromBlit(chr))
    r, g, b = r*n, g*n, b*n
    return {math.floor(r*255), math.floor(g*255), math.floor(b*255)}
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

local function get_color_normal(item)
    if item.is_block then
        local res = data
        for _, key in ipairs(split(item.block_type, ".")) do
            res = res[key]
            if res == nil then return fromBlitChar("b", item) end
        end
        if res == nil then return fromBlitChar("b", item) end
        return fromBlitChar(res, item)
    end

    return fromBlitChar("e", item)
end

local function get_color_depthmap(item)
    if (item.distance == nil) then return 255 end
    local num = (item.distance / max_distance) * 255
    return {math.floor(num), math.floor(num), math.floor(num)}
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


local x_axis = linspace(-width_roll_range, width_roll_range, width)
local y_axis = linspace(-height_pitch_range, height_pitch_range, height)

for y=1, height do
for x=1, width do
    yield()

    local yr = y_axis[y]
    local xr = x_axis[x]

    local item = rc.raycast(max_distance, {yr, xr, vector_fov}, euler_mode, true)
    local r, g, b = table.unpack(depth_map and get_color_depthmap(item) or get_color_normal(item))

    img.setRGB(x-2, height - y-1, r, g, b)
    gpu.drawImage(1, 1, img.ref())
    gpu.sync()
end end

--gpu.drawImage(1, 1, img.ref())
--gpu.sync()