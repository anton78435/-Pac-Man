// pacman.cpp
#include <curses.h>
#include <stdlib.h>
#include <time.h>
#include <unistd.h>
#include <fstream>
#include <string>
#include <vector>
#include <json/json.h>

using namespace std;

int loadRecord() {
    ifstream f(getenv("HOME") + string("/.pacman_record.json"));
    Json::Value root;
    if (f >> root) return root["record"].asInt();
    return 0;
}

void saveRecord(int record) {
    Json::Value root;
    root["record"] = record;
    ofstream f(getenv("HOME") + string("/.pacman_record.json"));
    f << root.toStyledString();
}

int main(int argc, char* argv[]) {
    int speed = 150;
    for (int i=1; i<argc; ++i) {
        if (string(argv[i]) == "-s" && i+1 < argc) speed = atoi(argv[++i]);
        else if (string(argv[i]) == "-h") { cout << "Usage: pacman [-s speed_ms]\n"; return 0; }
    }

    initscr();
    cbreak();
    noecho();
    curs_set(0);
    nodelay(stdscr, TRUE);
    keypad(stdscr, TRUE);
    start_color();
    init_pair(1, COLOR_YELLOW, COLOR_BLACK);
    init_pair(2, COLOR_RED, COLOR_BLACK);
    init_pair(3, COLOR_GREEN, COLOR_BLACK);
    init_pair(4, COLOR_CYAN, COLOR_BLACK);
    init_pair(5, COLOR_WHITE, COLOR_BLACK);
    init_pair(6, COLOR_BLUE, COLOR_BLACK);

    int height, width;
    getmaxyx(stdscr, height, width);
    if (height < 22 || width < 42) { endwin(); cout << "Terminal too small\n"; return 1; }

    const int H=20, W=20;
    int map[H][W] = {
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
        {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}
    };
    int pac_x=9, pac_y=9;
    int dir_x=0, dir_y=0;
    int score=0, lives=3, level=1;
    int best=loadRecord();
    bool gameOver=false;
    int frame = speed*1000;

    struct Ghost { int x,y,color,dir_x,dir_y; bool scared; int hx,hy; };
    Ghost ghosts[4] = {
        {9,8,2,1,0,false,9,8},
        {8,9,2,0,1,false,8,9},
        {10,9,2,0,-1,false,10,9},
        {9,10,2,-1,0,false,9,10}
    };
    int scared_timer=0;
    int total_dots=0;
    for (int y=0; y<H; ++y) for (int x=0; x<W; ++x) if (map[y][x]==1||map[y][x]==2) total_dots++;

    auto draw = [&]() {
        clear();
        for (int y=0; y<H; ++y) {
            for (int x=0; x<W; ++x) {
                int ch = map[y][x];
                if (ch==0) { mvaddch(y+1, x*2+1, '#'); }
                else if (ch==1) { attron(COLOR_PAIR(3)); mvaddch(y+1, x*2+1, '.'); attroff(COLOR_PAIR(3)); }
                else if (ch==2) { attron(COLOR_PAIR(4)); mvaddch(y+1, x*2+1, 'O'); attroff(COLOR_PAIR(4)); }
            }
        }
        attron(COLOR_PAIR(1)|A_BOLD);
        mvaddch(pac_y+1, pac_x*2+1, 'C');
        attroff(COLOR_PAIR(1)|A_BOLD);
        for (auto &g : ghosts) {
            int color = g.scared ? 6 : g.color;
            attron(COLOR_PAIR(color)|A_BOLD);
            mvaddch(g.y+1, g.x*2+1, 'G');
            attroff(COLOR_PAIR(color)|A_BOLD);
        }
        attron(COLOR_PAIR(5));
        mvprintw(0, 2, "Score: %d", score);
        mvprintw(0, W*2-12, "Best: %d", best);
        mvprintw(0, W*2-25, "Lives: %d  Level: %d", lives, level);
        if (gameOver) {
            const char* msg = "GAME OVER! Press R to restart, Q to quit";
            mvprintw(H/2+1, (W*2 - strlen(msg))/2, "%s", msg);
        }
        attroff(COLOR_PAIR(5));
        refresh();
    };

    while (true) {
        int ch = getch();
        if (ch=='q'||ch=='Q') break;
        if (ch=='r'||ch=='R') {
            if (gameOver) {
                for (int y=0; y<H; ++y) for (int x=0; x<W; ++x) map[y][x] = map[y][x];
                pac_x=9; pac_y=9; dir_x=dir_y=0;
                score=0; lives=3; level=1; gameOver=false; scared_timer=0;
                for (auto &g : ghosts) { g.x=g.hx; g.y=g.hy; g.dir_x=g.dir_y=0; g.scared=false; }
                total_dots=0; for (int y=0; y<H; ++y) for (int x=0; x<W; ++x) if (map[y][x]==1||map[y][x]==2) total_dots++;
                continue;
            }
        }

        if (gameOver) { draw(); continue; }

        if (ch==KEY_LEFT || ch=='a') { dir_x=0; dir_y=-1; }
        else if (ch==KEY_RIGHT || ch=='d') { dir_x=0; dir_y=1; }
        else if (ch==KEY_UP || ch=='w') { dir_x=-1; dir_y=0; }
        else if (ch==KEY_DOWN || ch=='s') { dir_x=1; dir_y=0; }

        int ny = pac_y + dir_x;
        int nx = pac_x + dir_y;
        if (ny>=0 && ny<H && nx>=0 && nx<W && map[ny][nx]!=0) {
            pac_y=ny; pac_x=nx;
            if (map[ny][nx]==1) { score+=10; map[ny][nx]=3; total_dots--; }
            else if (map[ny][nx]==2) { score+=50; map[ny][nx]=3; total_dots--; scared_timer=30; for (auto &g: ghosts) g.scared=true; }
        }

        for (auto &g : ghosts) {
            if (g.scared) {
                int dx = g.x - pac_x, dy = g.y - pac_y;
                // упрощённо – случайно
                int dirs[4][2] = {{1,0},{-1,0},{0,1},{0,-1}};
                for (int i=0; i<4; ++i) {
                    int idx = rand()%4;
                    int nx2 = g.x + dirs[idx][0];
                    int ny2 = g.y + dirs[idx][1];
                    if (nx2>=0 && nx2<W && ny2>=0 && ny2<H && map[ny2][nx2]!=0) {
                        g.x=nx2; g.y=ny2; break;
                    }
                }
            } else {
                int dirs[4][2] = {{1,0},{-1,0},{0,1},{0,-1}};
                int idx = rand()%4;
                int nx2 = g.x + dirs[idx][0];
                int ny2 = g.y + dirs[idx][1];
                if (nx2>=0 && nx2<W && ny2>=0 && ny2<H && map[ny2][nx2]!=0) {
                    g.x=nx2; g.y=ny2;
                }
            }
        }

        for (auto &g : ghosts) {
            if (g.x==pac_x && g.y==pac_y) {
                if (g.scared) {
                    score+=100;
                    g.x=g.hx; g.y=g.hy; g.scared=false;
                } else {
                    lives--;
                    if (lives<=0) { gameOver=true; if(score>best){best=score; saveRecord(best);} }
                    else { pac_x=9; pac_y=9; for (auto &g2: ghosts) { g2.x=g2.hx; g2.y=g2.hy; g2.scared=false; } }
                }
                break;
            }
        }

        if (scared_timer>0) { scared_timer--; if(scared_timer==0) for(auto &g: ghosts) g.scared=false; }

        if (total_dots==0) {
            level++;
            for (int y=0; y<H; ++y) for (int x=0; x<W; ++x) map[y][x] = map[y][x];
            total_dots=0; for (int y=0; y<H; ++y) for (int x=0; x<W; ++x) if (map[y][x]==1||map[y][x]==2) total_dots++;
            pac_x=9; pac_y=9;
            for (auto &g: ghosts) { g.x=g.hx; g.y=g.hy; g.scared=false; }
        }

        draw();
        usleep(frame);
    }
    endwin();
    return 0;
}
