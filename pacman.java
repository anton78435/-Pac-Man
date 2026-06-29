// pacman.java
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import com.google.gson.*;

public class pacman {
    private static String configFile = System.getProperty("user.home") + "/.pacman_record.json";
    private static int loadRecord() throws IOException {
        Path path = Paths.get(configFile);
        if (!Files.exists(path)) return 0;
        String json = new String(Files.readAllBytes(path));
        JsonObject obj = new Gson().fromJson(json, JsonObject.class);
        return obj.get("record").getAsInt();
    }
    private static void saveRecord(int record) throws IOException {
        JsonObject obj = new JsonObject();
        obj.addProperty("record", record);
        Files.write(Paths.get(configFile), new GsonBuilder().setPrettyPrinting().create().toJson(obj).getBytes());
    }

    public static void main(String[] args) throws Exception {
        int speed = 150;
        for (int i=0; i<args.length; i++) {
            if (args[i].equals("-s") && i+1 < args.length) speed = Integer.parseInt(args[++i]);
            else if (args[i].equals("-h")) { System.out.println("Usage: pacman [-s speed_ms]"); return; }
        }
        Terminal terminal = new DefaultTerminalFactory().createTerminal();
        terminal.enterPrivateMode();
        terminal.setCursorVisible(false);
        TerminalSize size = terminal.getTerminalSize();
        int height = size.getRows();
        int width = size.getColumns();
        if (height < 22 || width < 42) { System.out.println("Terminal too small"); System.exit(1); }

        Random rand = new Random();
        int H=20, W=20;
        int[][] mapData = {
            {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
            {0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0},
            {0,1,0,1,0,0,1,0,0,1,1,0,0,1,0,0,1,0,1,0},
            {0,2,0,1,0,0,1,0,0,1,1,0,0,1,0,0,1,0,2,0},
            {0,1,0,1,0,0,1,0,0,1,1,0,0,1,0,0,1,0,1,0},
            {0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0},
            {0,1,0,0,1,0,0,1,0,0,0,0,1,0,0,1,0,0,1,0},
            {0,1,1,1,1,0,0,1,0,0,0,0,1,0,0,1,1,1,1,0},
            {0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0},
            {0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0},
            {0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0},
            {0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0},
            {0,1,1,1,1,0,0,1,0,0,0,0,1,0,0,1,1,1,1,0},
            {0,1,0,0,1,0,0,1,0,0,0,0,1,0,0,1,0,0,1,0},
            {0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0},
            {0,1,0,1,0,0,1,0,0,1,1,0,0,1,0,0,1,0,1,0},
            {0,2,0,1,0,0,1,0,0,1,1,0,0,1,0,0,1,0,2,0},
            {0,1,0,1,0,0,1,0,0,1,1,0,0,1,0,0,1,0,1,0},
            {0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0},
            {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
        };
        int pacX=9, pacY=9;
        int dirX=0, dirY=0;
        int score=0, lives=3, level=1;
        int best=loadRecord();
        boolean gameOver=false;
        int frame=speed;

        List<Map<String,Object>> ghosts = new ArrayList<>();
        ghosts.add(new HashMap<String,Object>(){{put("x",9); put("y",8); put("hx",9); put("hy",8); put("scared",false);}});
        ghosts.add(new HashMap<String,Object>(){{put("x",8); put("y",9); put("hx",8); put("hy",9); put("scared",false);}});
        ghosts.add(new HashMap<String,Object>(){{put("x",10); put("y",9); put("hx",10); put("hy",9); put("scared",false);}});
        ghosts.add(new HashMap<String,Object>(){{put("x",9); put("y",10); put("hx",9); put("hy",10); put("scared",false);}});
        int scaredTimer=0;
        int totalDots=0;
        for (int y=0; y<H; y++) for (int x=0; x<W; x++) if (mapData[y][x]==1||mapData[y][x]==2) totalDots++;

        TextGraphics tg = terminal.newTextGraphics();

        while (true) {
            KeyStroke key = terminal.pollInput();
            if (key != null) {
                char ch = key.getCharacter() != null ? key.getCharacter() : 0;
                if (ch == 'q' || ch == 'Q') break;
                if (ch == 'r' || ch == 'R') {
                    if (gameOver) {
                        // reset (copy initial map)
                        int[][] newMap = {
                            {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
                            {0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0},
                            {0,1,0,1,0,0,1,0,0,1,1,0,0,1,0,0,1,0,1,0},
                            {0,2,0,1,0,0,1,0,0,1,1,0,0,1,0,0,1,0,2,0},
                            {0,1,0,1,0,0,1,0,0,1,1,0,0,1,0,0,1,0,1,0},
                            {0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0},
                            {0,1,0,0,1,0,0,1,0,0,0,0,1,0,0,1,0,0,1,0},
                            {0,1,1,1,1,0,0,1,0,0,0,0,1,0,0,1,1,1,1,0},
                            {0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0},
                            {0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0},
                            {0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0},
                            {0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0},
                            {0,1,1,1,1,0,0,1,0,0,0,0,1,0,0,1,1,1,1,0},
                            {0,1,0,0,1,0,0,1,0,0,0,0,1,0,0,1,0,0,1,0},
                            {0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0},
                            {0,1,0,1,0,0,1,0,0,1,1,0,0,1,0,0,1,0,1,0},
                            {0,2,0,1,0,0,1,0,0,1,1,0,0,1,0,0,1,0,2,0},
                            {0,1,0,1,0,0,1,0,0,1,1,0,0,1,0,0,1,0,1,0},
                            {0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0},
                            {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
                        };
                        mapData = newMap;
                        pacX=9; pacY=9; dirX=0; dirY=0;
                        score=0; lives=3; level=1; gameOver=false; scaredTimer=0;
                        ghosts.clear();
                        ghosts.add(new HashMap<String,Object>(){{put("x",9); put("y",8); put("hx",9); put("hy",8); put("scared",false);}});
                        ghosts.add(new HashMap<String,Object>(){{put("x",8); put("y",9); put("hx",8); put("hy",9); put("scared",false);}});
                        ghosts.add(new HashMap<String,Object>(){{put("x",10); put("y",9); put("hx",10); put("hy",9); put("scared",false);}});
                        ghosts.add(new HashMap<String,Object>(){{put("x",9); put("y",10); put("hx",9); put("hy",10); put("scared",false);}});
                        totalDots=0; for (int y=0; y<H; y++) for (int x=0; x<W; x++) if (mapData[y][x]==1||mapData[y][x]==2) totalDots++;
                        continue;
                    }
                }
                if (key.getKeyType() == KeyStroke.KeyType.ArrowLeft || ch == 'a') { dirX=0; dirY=-1; }
                if (key.getKeyType() == KeyStroke.KeyType.ArrowRight || ch == 'd') { dirX=0; dirY=1; }
                if (key.getKeyType() == KeyStroke.KeyType.ArrowUp || ch == 'w') { dirX=-1; dirY=0; }
                if (key.getKeyType() == KeyStroke.KeyType.ArrowDown || ch == 's') { dirX=1; dirY=0; }
            }

            if (gameOver) {
                tg.clear();
                String msg = "GAME OVER! Score: " + score + "  Best: " + best;
                tg.putString((width - msg.length())/2, height/2-2, msg, TextColor.ANSI.RED);
                tg.putString((width - 20)/2, height/2, "R - restart | Q - quit", TextColor.ANSI.CYAN);
                terminal.flush();
                continue;
            }

            // Move Pac-Man
            int ny = pacY + dirX;
            int nx = pacX + dirY;
            if (ny>=0 && ny<H && nx>=0 && nx<W && mapData[ny][nx] != 0) {
                pacY=ny; pacX=nx;
                if (mapData[ny][nx] == 1) { score+=10; mapData[ny][nx]=3; totalDots--; }
                else if (mapData[ny][nx] == 2) { score+=50; mapData[ny][nx]=3; totalDots--; scaredTimer=30; for (Map<String,Object> g : ghosts) g.put("scared", true); }
            }

            // Ghosts movement
            int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
            for (Map<String,Object> g : ghosts) {
                int[] d = dirs[rand.nextInt(4)];
                int nx2 = (int)g.get("x") + d[0];
                int ny2 = (int)g.get("y") + d[1];
                if (nx2>=0 && nx2<W && ny2>=0 && ny2<H && mapData[ny2][nx2] != 0) {
                    g.put("x", nx2);
                    g.put("y", ny2);
                }
            }

            // Collisions
            for (Map<String,Object> g : ghosts) {
                if ((int)g.get("x") == pacX && (int)g.get("y") == pacY) {
                    if ((boolean)g.get("scared")) {
                        score += 100;
                        g.put("x", g.get("hx"));
                        g.put("y", g.get("hy"));
                        g.put("scared", false);
                    } else {
                        lives--;
                        if (lives <= 0) {
                            gameOver = true;
                            if (score > best) { best = score; saveRecord(best); }
                        } else {
                            pacX=9; pacY=9;
                            for (Map<String,Object> g2 : ghosts) {
                                g2.put("x", g2.get("hx"));
                                g2.put("y", g2.get("hy"));
                                g2.put("scared", false);
                            }
                        }
                    }
                    break;
                }
            }

            if (scaredTimer > 0) {
                scaredTimer--;
                if (scaredTimer == 0) for (Map<String,Object> g : ghosts) g.put("scared", false);
            }

            if (totalDots == 0) {
                level++;
                // reset map (copy initial)
                int[][] newMap = {
                    {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
                    {0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0},
                    {0,1,0,1,0,0,1,0,0,1,1,0,0,1,0,0,1,0,1,0},
                    {0,2,0,1,0,0,1,0,0,1,1,0,0,1,0,0,1,0,2,0},
                    {0,1,0,1,0,0,1,0,0,1,1,0,0,1,0,0,1,0,1,0},
                    {0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0},
                    {0,1,0,0,1,0,0,1,0,0,0,0,1,0,0,1,0,0,1,0},
                    {0,1,1,1,1,0,0,1,0,0,0,0,1,0,0,1,1,1,1,0},
                    {0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0},
                    {0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0},
                    {0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0},
                    {0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0},
                    {0,1,1,1,1,0,0,1,0,0,0,0,1,0,0,1,1,1,1,0},
                    {0,1,0,0,1,0,0,1,0,0,0,0,1,0,0,1,0,0,1,0},
                    {0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0},
                    {0,1,0,1,0,0,1,0,0,1,1,0,0,1,0,0,1,0,1,0},
                    {0,2,0,1,0,0,1,0,0,1,1,0,0,1,0,0,1,0,2,0},
                    {0,1,0,1,0,0,1,0,0,1,1,0,0,1,0,0,1,0,1,0},
                    {0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0},
                    {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
                };
                mapData = newMap;
                totalDots=0; for (int y=0; y<H; y++) for (int x=0; x<W; x++) if (mapData[y][x]==1||mapData[y][x]==2) totalDots++;
                pacX=9; pacY=9;
                for (Map<String,Object> g : ghosts) {
                    g.put("x", g.get("hx"));
                    g.put("y", g.get("hy"));
                    g.put("scared", false);
                }
            }

            // Draw
            tg.clear();
            for (int y=0; y<H; y++) {
                for (int x=0; x<W; x++) {
                    int v = mapData[y][x];
                    if (v==0) tg.putString(x*2+1, y+1, "#", TextColor.ANSI.WHITE);
                    else if (v==1) tg.putString(x*2+1, y+1, ".", TextColor.ANSI.GREEN);
                    else if (v==2) tg.putString(x*2+1, y+1, "O", TextColor.ANSI.CYAN);
                }
            }
            tg.putString(pacX*2+1, pacY+1, "C", TextColor.ANSI.YELLOW);
            for (Map<String,Object> g : ghosts) {
                TextColor color = (boolean)g.get("scared") ? TextColor.ANSI.BLUE : TextColor.ANSI.RED;
                tg.putString((int)g.get("x")*2+1, (int)g.get("y")+1, "G", color);
            }
            tg.putString(2, 0, "Score: " + score, TextColor.ANSI.WHITE);
            tg.putString(W*2-12, 0, "Best: " + best, TextColor.ANSI.WHITE);
            tg.putString(W*2-25, 0, "Lives: " + lives + "  Level: " + level, TextColor.ANSI.WHITE);
            if (gameOver) {
                String msg = "GAME OVER! Press R to restart, Q to quit";
                tg.putString((W*2 - msg.length())/2, H/2+1, msg, TextColor.ANSI.RED);
            }
            terminal.flush();
            Thread.sleep(frame);
        }
        terminal.exitPrivateMode();
        terminal.close();
    }
}
