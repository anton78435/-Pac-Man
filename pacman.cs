// pacman.cs
using System;
using System.Collections.Generic;
using System.IO;
using System.Text.Json;
using System.Threading;
using System.Runtime.InteropServices;

class Pacman
{
    static string Colorize(string text, string color)
    {
        string col = color switch
        {
            "yellow" => "\x1b[93m",
            "red" => "\x1b[91m",
            "green" => "\x1b[92m",
            "cyan" => "\x1b[96m",
            "white" => "\x1b[97m",
            "blue" => "\x1b[94m",
            _ => "\x1b[0m"
        };
        return col + text + "\x1b[0m";
    }

    static string ConfigFile => Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.UserProfile), ".pacman_record.json");
    static int LoadRecord()
    {
        if (!File.Exists(ConfigFile)) return 0;
        var data = JsonSerializer.Deserialize<Dictionary<string,int>>(File.ReadAllText(ConfigFile));
        return data.GetValueOrDefault("record", 0);
    }
    static void SaveRecord(int record)
    {
        var data = new Dictionary<string,int>{ {"record", record} };
        File.WriteAllText(ConfigFile, JsonSerializer.Serialize(data));
    }

    static void Main(string[] args)
    {
        int speed = 150;
        for (int i=0; i<args.Length; i++)
        {
            if (args[i] == "-s" && i+1 < args.Length) speed = int.Parse(args[++i]);
            else if (args[i] == "-h") { Console.WriteLine("Usage: pacman [-s speed_ms]"); return; }
        }
        Console.Clear();
        int height = Console.WindowHeight;
        int width = Console.WindowWidth;
        if (height < 22 || width < 42) { Console.WriteLine("Terminal too small"); return; }
        Random rand = new Random();
        int H=20, W=20;
        int[,] mapData = {
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
        int best=LoadRecord();
        bool gameOver=false;
        int frame = speed;

        List<(int x,int y,int homeX,int homeY,bool scared)> ghosts = new List<(int,int,int,int,bool)>();
        ghosts.Add((9,8,9,8,false));
        ghosts.Add((8,9,8,9,false));
        ghosts.Add((10,9,10,9,false));
        ghosts.Add((9,10,9,10,false));
        int scaredTimer=0;
        int totalDots = 0;
        for (int y=0; y<H; y++) for (int x=0; x<W; x++) if (mapData[y,x]==1||mapData[y,x]==2) totalDots++;

        Console.CursorVisible = false;
        while (true)
        {
            if (Console.KeyAvailable)
            {
                var key = Console.ReadKey(true).Key;
                if (key == ConsoleKey.Q) break;
                if (key == ConsoleKey.R && gameOver)
                {
                    // reset
                    int[,] mapData2 = {
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
                    mapData = mapData2;
                    pacX=9; pacY=9; dirX=dirY=0;
                    score=0; lives=3; level=1; gameOver=false; scaredTimer=0;
                    ghosts.Clear();
                    ghosts.Add((9,8,9,8,false));
                    ghosts.Add((8,9,8,9,false));
                    ghosts.Add((10,9,10,9,false));
                    ghosts.Add((9,10,9,10,false));
                    totalDots = 0;
                    for (int y=0; y<H; y++) for (int x=0; x<W; x++) if (mapData[y,x]==1||mapData[y,x]==2) totalDots++;
                    continue;
                }
                if (key == ConsoleKey.LeftArrow || key == ConsoleKey.A) { dirX=0; dirY=-1; }
                if (key == ConsoleKey.RightArrow || key == ConsoleKey.D) { dirX=0; dirY=1; }
                if (key == ConsoleKey.UpArrow || key == ConsoleKey.W) { dirX=-1; dirY=0; }
                if (key == ConsoleKey.DownArrow || key == ConsoleKey.S) { dirX=1; dirY=0; }
            }

            if (gameOver)
            {
                Console.Clear();
                string msg = $"GAME OVER! Score: {score}  Best: {best}";
                Console.SetCursorPosition((width - msg.Length)/2, height/2-2);
                Console.Write(Colorize(msg, "red"));
                Console.SetCursorPosition((width-20)/2, height/2);
                Console.Write(Colorize("R - restart | Q - quit", "cyan"));
                continue;
            }

            // Move Pac-Man
            int ny = pacY + dirX;
            int nx = pacX + dirY;
            if (ny>=0 && ny<H && nx>=0 && nx<W && mapData[ny,nx] != 0)
            {
                pacY=ny; pacX=nx;
                if (mapData[ny,nx] == 1) { score += 10; mapData[ny,nx] = 3; totalDots--; }
                else if (mapData[ny,nx] == 2) { score += 50; mapData[ny,nx] = 3; totalDots--; scaredTimer = 30; for (int i=0; i<ghosts.Count; i++) { var g=ghosts[i]; ghosts[i] = (g.x,g.y,g.homeX,g.homeY,true); } }
            }

            // Ghosts movement
            int[,] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
            for (int i=0; i<ghosts.Count; i++)
            {
                var g = ghosts[i];
                int idx = rand.Next(4);
                int dx = dirs[idx,0], dy = dirs[idx,1];
                int nx2 = g.x + dx, ny2 = g.y + dy;
                if (nx2>=0 && nx2<W && ny2>=0 && ny2<H && mapData[ny2,nx2] != 0)
                {
                    g.x = nx2; g.y = ny2;
                    ghosts[i] = g;
                }
            }

            // Collisions
            for (int i=0; i<ghosts.Count; i++)
            {
                var g = ghosts[i];
                if (g.x == pacX && g.y == pacY)
                {
                    if (g.scared)
                    {
                        score += 100;
                        g.x = g.homeX; g.y = g.homeY;
                        g.scared = false;
                        ghosts[i] = g;
                    }
                    else
                    {
                        lives--;
                        if (lives <= 0) { gameOver = true; if (score>best){best=score; SaveRecord(best);} }
                        else
                        {
                            pacX=9; pacY=9;
                            for (int j=0; j<ghosts.Count; j++)
                            {
                                var g2 = ghosts[j];
                                g2.x = g2.homeX; g2.y = g2.homeY;
                                g2.scared = false;
                                ghosts[j] = g2;
                            }
                        }
                    }
                    break;
                }
            }

            if (scaredTimer > 0)
            {
                scaredTimer--;
                if (scaredTimer == 0)
                    for (int i=0; i<ghosts.Count; i++)
                    {
                        var g = ghosts[i];
                        g.scared = false;
                        ghosts[i] = g;
                    }
            }

            if (totalDots == 0)
            {
                level++;
                // reset map
                int[,] mapData2 = {
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
                mapData = mapData2;
                totalDots = 0; for (int y=0; y<H; y++) for (int x=0; x<W; x++) if (mapData[y,x]==1||mapData[y,x]==2) totalDots++;
                pacX=9; pacY=9;
                for (int i=0; i<ghosts.Count; i++)
                {
                    var g = ghosts[i];
                    g.x = g.homeX; g.y = g.homeY;
                    g.scared = false;
                    ghosts[i] = g;
                }
            }

            // Draw
            Console.Clear();
            for (int y=0; y<H; y++)
            {
                for (int x=0; x<W; x++)
                {
                    int v = mapData[y,x];
                    if (v==0) { Console.SetCursorPosition(x*2+1, y+1); Console.Write(Colorize("#","white")); }
                    else if (v==1) { Console.SetCursorPosition(x*2+1, y+1); Console.Write(Colorize(".","green")); }
                    else if (v==2) { Console.SetCursorPosition(x*2+1, y+1); Console.Write(Colorize("O","cyan")); }
                }
            }
            Console.SetCursorPosition(pacX*2+1, pacY+1);
            Console.Write(Colorize("C","yellow"));
            foreach (var g in ghosts)
            {
                string col = g.scared ? "blue" : "red";
                Console.SetCursorPosition(g.x*2+1, g.y+1);
                Console.Write(Colorize("G", col));
            }
            Console.SetCursorPosition(2, 0);
            Console.Write(Colorize($"Score: {score}", "white"));
            Console.SetCursorPosition(W*2-12, 0);
            Console.Write(Colorize($"Best: {best}", "white"));
            Console.SetCursorPosition(W*2-25, 0);
            Console.Write(Colorize($"Lives: {lives}  Level: {level}", "white"));
            if (gameOver)
            {
                string msg = "GAME OVER! Press R to restart, Q to quit";
                Console.SetCursorPosition((W*2 - msg.Length)/2, H/2+1);
                Console.Write(Colorize(msg, "red"));
            }
            Thread.Sleep(frame);
        }
    }
}
