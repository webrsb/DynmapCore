package org.dynmap.hdmap;

import static org.dynmap.JSONUtils.a;
import static org.dynmap.JSONUtils.s;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.dynmap.Client;
import org.dynmap.ConfigurationNode;
import org.dynmap.DynmapChunk;
import org.dynmap.DynmapCore;
import org.dynmap.DynmapWorld;
import org.dynmap.Log;
import org.dynmap.MapManager;
import org.dynmap.MapTile;
import org.dynmap.MapType;
import org.dynmap.debug.Debug;
import org.dynmap.utils.TileFlags;
import org.json.simple.JSONObject;

public class HDMap extends MapType {

    private String name;
    private String prefix;
    private HDPerspective perspective;
    private HDShader shader;
    private HDLighting lighting;
//    private ConfigurationNode configuration;
    private int mapzoomout;
    private String imgfmtstring;
    private MapType.ImageFormat imgformat;
    private int bgcolornight;
    private int bgcolorday;
    private String title;
    private String icon;
    private String bg_cfg;
    private String bg_day_cfg;
    private String bg_night_cfg;
    private String append_to_world;
    private int mapzoomin;
    public DynmapCore core;

    public static final String IMGFORMAT_PNG = "png";
    public static final String IMGFORMAT_JPG = "jpg";
    
    
    public HDMap(DynmapCore core, ConfigurationNode configuration) {
        this.core = core;
        name = configuration.getString("name", null);
        if(name == null) {
            Log.severe("HDMap missing required attribute 'name' - disabled");
            return;
        }
        String perspectiveid = configuration.getString("perspective", "default");
        perspective = MapManager.mapman.hdmapman.perspectives.get(perspectiveid);
        if(perspective == null) {
            /* Try to use default */
            perspective = MapManager.mapman.hdmapman.perspectives.get("default");
            if(perspective == null) {
                Log.severe("HDMap '"+name+"' loaded invalid perspective '" + perspectiveid + "' - map disabled");
                name = null;
                return;
            }
            else {
                Log.severe("HDMap '"+name+"' loaded invalid perspective '" + perspectiveid + "' - using 'default' perspective");
            }
        }
        String shaderid = configuration.getString("shader", "default");
        shader = MapManager.mapman.hdmapman.shaders.get(shaderid);
        if(shader == null) {
            shader = MapManager.mapman.hdmapman.shaders.get("default");
            if(shader == null) {
                Log.severe("HDMap '"+name+"' loading invalid shader '" + shaderid + "' - map disabled");
                name = null;
                return;
            }
            else {
                Log.severe("HDMap '"+name+"' loading invalid shader '" + shaderid + "' - using 'default' shader");
            }
        }
        String lightingid = configuration.getString("lighting", "default");
        lighting = MapManager.mapman.hdmapman.lightings.get(lightingid);
        if(lighting == null) {
            lighting = MapManager.mapman.hdmapman.lightings.get("default");
            if(lighting == null) {
                Log.severe("HDMap '"+name+"' loading invalid lighting '" + lighting + "' - map disabled");
                name = null;
                return;
            }
            else {
                Log.severe("HDMap '"+name+"' loading invalid lighting '" + lighting + "' - using 'default' lighting");
            }
        }
        prefix = configuration.getString("prefix", name);
//        this.configuration = configuration;
        
        /* Compute extra zoom outs needed for this map */
        double scale = perspective.getScale();
        mapzoomout = 0;
        while(scale >= 1.0) {
            mapzoomout++;
            scale = scale / 2.0;
        }
        imgfmtstring = configuration.getString("image-format", "default");
        if(imgfmtstring.equals("default")) {
            imgformat = ImageFormat.fromID(core.getDefImageFormat());
        }
        else {
            imgformat = ImageFormat.fromID(imgfmtstring);
        }
        if(imgformat == null) {
            Log.severe("HDMap '"+name+"' set invalid image-format: " + imgfmtstring);
            imgformat = ImageFormat.FORMAT_PNG;
        }
        /* Get color info */
        String c = configuration.getString("background");
        if(c != null) {
            bgcolorday = bgcolornight = parseColor(c);
        }
        c = configuration.getString("backgroundday");
        if(c != null) {
            bgcolorday = parseColor(c);
        }
        c = configuration.getString("backgroundnight");
        if(c != null) {
            bgcolornight = parseColor(c);
        }
        if(imgformat != ImageFormat.FORMAT_PNG) {   /* If JPG, set background color opacity */
            bgcolorday |= 0xFF000000;
            bgcolornight |= 0xFF000000;
        }
        this.title = configuration.getString("title", name);
        this.icon = configuration.getString("icon");
        this.bg_cfg = configuration.getString("background");
        this.bg_day_cfg = configuration.getString("backgroundday");
        this.bg_night_cfg = configuration.getString("backgroundnight");
        this.mapzoomin = configuration.getInteger("mapzoomin", 2);
        this.append_to_world = configuration.getString("append_to_world", "");
        setProtected(configuration.getBoolean("protected", false));
        setTileUpdateDelay(configuration.getInteger("tileupdatedelay", -1));
    }

    public ConfigurationNode saveConfiguration() {
        ConfigurationNode cn = super.saveConfiguration();
        cn.put("title", title);
        if(icon != null)
            cn.put("icon", icon);
        cn.put("prefix", prefix);
        if(perspective != null)
            cn.put("perspective", perspective.getName());
        if(shader != null)
            cn.put("shader", shader.getName());
        if(lighting != null)
            cn.put("lighting", lighting.getName());
        cn.put("image-format", imgfmtstring);
        cn.put("mapzoomin", mapzoomin);
        if(bg_cfg != null)
            cn.put("background", bg_cfg);
        if(bg_day_cfg != null)
            cn.put("backgroundday", bg_day_cfg);
        if(bg_night_cfg != null)
            cn.put("backgroundnight", bg_night_cfg);
        cn.put("append_to_world", append_to_world);
        cn.put("protected", isProtected());
        if(this.tileupdatedelay > 0) {
            cn.put("tileupdatedelay", this.tileupdatedelay);
        }
        return cn;
    }
    
    public HDShader getShader() { return shader; }
    public HDPerspective getPerspective() { return perspective; }
    public HDLighting getLighting() { return lighting; }
    
    @Override
    public List<TileFlags.TileCoord> getTileCoords(DynmapWorld w, int x, int y, int z) {
        return perspective.getTileCoords(w, x, y, z);
    }

    @Override
    public List<TileFlags.TileCoord> getTileCoords(DynmapWorld w, int minx, int miny, int minz, int maxx, int maxy, int maxz) {
        return perspective.getTileCoords(w, minx, miny, minz, maxx, maxy, maxz);
    }

    @Override
    public MapTile[] getAdjecentTiles(MapTile tile) {
        return perspective.getAdjecentTiles(tile);
    }
    
    @Override
    public List<DynmapChunk> getRequiredChunks(MapTile tile) {
        return perspective.getRequiredChunks(tile);
    }

    @Override
    public List<ZoomInfo> baseZoomFileInfo() {
        ArrayList<ZoomInfo> s = new ArrayList<ZoomInfo>();
        s.add(new ZoomInfo(prefix, getBackgroundARGBNight()));
        if(lighting.isNightAndDayEnabled())
            s.add(new ZoomInfo(prefix + "_day", getBackgroundARGBDay()));
        return s;
    }

    public int baseZoomFileStepSize() { return 1; }

    private static final int[] stepseq = { 3, 1, 2, 0 };
    
    public MapStep zoomFileMapStep() { return MapStep.X_PLUS_Y_MINUS; }

    public int[] zoomFileStepSequence() { return stepseq; }

    /* How many bits of coordinate are shifted off to make big world directory name */
    public int getBigWorldShift() { return 5; }

    /* Returns true if big world file structure is in effect for this map */
    @Override
    public boolean isBigWorldMap(DynmapWorld w) { return true; } /* We always use it on these maps */

    /* Return number of zoom levels needed by this map (before extra levels from extrazoomout) */
    public int getMapZoomOutLevels() {
        return mapzoomout;
    }

    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public String getPrefix() {
        return prefix;
    }

    /* Get maps rendered concurrently with this map in this world */
    public List<MapType> getMapsSharingRender(DynmapWorld w) {
        ArrayList<MapType> maps = new ArrayList<MapType>();
        for(MapType mt : w.maps) {
            if(mt instanceof HDMap) {
                HDMap hdmt = (HDMap)mt;
                if(hdmt.perspective == this.perspective) {  /* Same perspective */
                    maps.add(hdmt);
                }
            }
        }
        return maps;
    }
    
    /* Get names of maps rendered concurrently with this map type in this world */
    public List<String> getMapNamesSharingRender(DynmapWorld w) {
        ArrayList<String> lst = new ArrayList<String>();
        for(MapType mt : w.maps) {
            if(mt instanceof HDMap) {
                HDMap hdmt = (HDMap)mt;
                if(hdmt.perspective == this.perspective) {  /* Same perspective */
                    if(hdmt.lighting.isNightAndDayEnabled())
                        lst.add(hdmt.getName() + "(night/day)");
                    else
                        lst.add(hdmt.getName());
                }
            }
        }
        return lst;
    }

    @Override
    public ImageFormat getImageFormat() { return imgformat; }
    
    @Override
    public void buildClientConfiguration(JSONObject worldObject, DynmapWorld world) {
        JSONObject o = new JSONObject();
        s(o, "type", "HDMapType");
        s(o, "name", name);
        s(o, "title", title);
        s(o, "icon", icon);
        s(o, "prefix", prefix);
        s(o, "background", bg_cfg);
        s(o, "backgroundday", bg_day_cfg);
        s(o, "backgroundnight", bg_night_cfg);
        s(o, "bigmap", true);
        s(o, "mapzoomout", (world.getExtraZoomOutLevels()+mapzoomout));
        s(o, "mapzoomin", mapzoomin);
        s(o, "protected", isProtected());
        s(o, "image-format", imgformat.getFileExt());
        if(append_to_world.length() > 0)
            s(o, "append_to_world", append_to_world);
        perspective.addClientConfiguration(o);
        shader.addClientConfiguration(o);
        lighting.addClientConfiguration(o);
        
        a(worldObject, "maps", o);

    }
    
    private static int parseColor(String c) {
        int v = 0;
        if(c.startsWith("#")) {
            c = c.substring(1);
            if(c.length() == 3) {   /* #rgb */
                try {
                    v = Integer.valueOf(c, 16);
                } catch (NumberFormatException nfx) {
                    return 0;
                }
                v = 0xFF000000 | ((v & 0xF00) << 12) | ((v & 0x0F0) << 8) | ((v & 0x00F) << 4);
            }
            else if(c.length() == 6) {  /* #rrggbb */
                try {
                    v = Integer.valueOf(c, 16);
                } catch (NumberFormatException nfx) {
                    return 0;
                }
                v = 0xFF000000 | (v & 0xFFFFFF);
            }
        }

        return v;
    }
    
    public int getBackgroundARGBDay() {
        return bgcolorday;
    }
    
    public int getBackgroundARGBNight() {
        return bgcolornight;
    }
    
    private HDMapTile fileToTile(DynmapWorld world, File f) {
        String n = f.getName();
        n = n.substring(0, n.lastIndexOf('.'));
        if(n == null) return null;
        String[] nt = n.split("_");
        if(nt.length != 2) return null;
        int xx, zz;
        try {
            xx = Integer.parseInt(nt[0]);
            zz = Integer.parseInt(nt[1]);
        } catch (NumberFormatException nfx) {
            return null;
        }
        return new HDMapTile(world, perspective, xx, zz);
    }
    
    public void purgeOldTiles(final DynmapWorld world, final TileFlags rendered) {
        File basedir = new File(world.worldtilepath, prefix);   /* Get base directory for map */
        FileCallback cb = new FileCallback() {
            public void fileFound(File f, File parent, boolean day) {
                String n = f.getName();
                if(n.startsWith("z")) { /* If zoom file */
                    if(n.startsWith("z_")) {    /* First tier of zoom? */
                        File ff = new File(parent, n.substring(2)); /* Make file for render tier, and drive update */
                        HDMapTile tile = fileToTile(world, ff); /* Parse it */
                        if(tile == null) return;
                        if(rendered.getFlag(tile.tx, tile.ty) || rendered.getFlag(tile.tx+1, tile.ty) ||
                                rendered.getFlag(tile.tx, tile.ty-1) || rendered.getFlag(tile.tx+1, tile.ty-1))
                            return;
                        world.enqueueZoomOutUpdate(ff);
                    }
                    return;
                }
                HDMapTile tile = fileToTile(world, f);
                if(tile == null) return;

                if(rendered.getFlag(tile.tx, tile.ty)) {  /* If we rendered this tile, its good */
                    return;
                }
                Debug.debug("clean up " + f.getPath());
                /* Otherwise, delete tile */
                f.delete();
                /* Push updates, clear hash code, and signal zoom tile update */
                MapManager.mapman.pushUpdate(world, 
                                             new Client.Tile(day?tile.getDayFilename(prefix, getImageFormat()):tile.getFilename(prefix, getImageFormat())));
                MapManager.mapman.hashman.updateHashCode(tile.getKey(prefix), day?"day":null, tile.tx, tile.ty, -1);
                world.enqueueZoomOutUpdate(f);
            }
                
        };
        walkMapTree(basedir, cb, false);
        if(lighting.isNightAndDayEnabled()) {
            basedir = new File(world.worldtilepath, prefix+"_day");
            walkMapTree(basedir, cb, true);
        }
    }
    
    public String getTitle() {
        return title;
    }
    public int getMapZoomIn() {
        return mapzoomin;
    }
    public String getIcon() {
        return (icon == null)?"":icon;
    }
    
    public boolean setPrefix(String s) {
        if(!s.equals(prefix)) {
            prefix = s;
            return true;
        }
        return false;
    }
    
    public boolean setTitle(String s) {
        if(!s.equals(title)) {
            title = s;
            return true;
        }
        return false;
    }
    public boolean setAppendToWorld(String s) {
        if(!s.equals(append_to_world)) {
            append_to_world = s;
            return true;
        }
        return false;
    }
    public String getAppendToWorld() {
        return append_to_world;
    }

    public boolean setMapZoomIn(int mzi) {
        if(mzi != mapzoomin) {
            mapzoomin = mzi;
            return true;
        }
        return false;
    }
    public boolean setPerspective(HDPerspective p) {
        if(perspective != p) {
            perspective = p;
            return true;
        }
        return false;
    }
    public boolean setShader(HDShader p) {
        if(shader != p) {
            shader = p;
            return true;
        }
        return false;
    }
    public boolean setLighting(HDLighting p) {
        if(lighting != p) {
            lighting = p;
            return true;
        }
        return false;
    }
    public boolean setImageFormatSetting(String f) {
        if(imgfmtstring.equals(f) == false) {
            MapType.ImageFormat newfmt;
            if(f.equals("default"))
                newfmt = MapType.ImageFormat.fromID(core.getDefImageFormat());
            else
                newfmt = MapType.ImageFormat.fromID(f);
            if(newfmt != null) {
                imgformat = newfmt;
                imgfmtstring = f;
                return true;
            }
        }
        return false;
    }
    public String getImageFormatSetting() {
        return imgfmtstring;
    }
    public boolean setIcon(String v) {
        if("".equals(v)) v = null;
        icon = v;
        return true;
    }

    @Override
    public void addMapTiles(List<MapTile> list, DynmapWorld w, int tx, int ty) {
        list.add(new HDMapTile(w, this.perspective, tx, ty));
    }
}
