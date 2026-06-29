# pacman.py
#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import sys, os, random, json, time, argparse, curses
from pathlib import Path

RECORD_FILE = Path.home() / '.pacman_record.json'

def load_record():
    try:
        with open(RECORD_FILE) as f:
            return json.load(f).get('record', 0)
    except:
        return 0

def save_record(record):
    with open(RECORD_FILE, 'w') as f:
        json.dump({'record': record}, f)

# Карта: 0-стена, 1-точка, 2-суперточка, 3-пусто
MAP = [
    [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],
    [0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0],
    [0,1,0,1,0,0,1,0,0,1,1,0,0,1,0,0,1,0,1,0],
    [0,2,0,1,0,0,1,0,0,1,1,0,0,1,0,0,1,0,2,0],
    [0,1,0,1,0,0,1,0,0,1,1,0,0,1,0,0,1,0,1,0],
    [0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0],
    [0,1,0,0,1,0,0,1,0,0,0,0,1,0,0,1,0,0,1,0],
    [0,1,1,1,1,0,0,1,0,0,0,0,1,0,0,1,1,1,1,0],
    [0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0],
    [0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0],
    [0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0],
    [0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0],
    [0,1,1,1,1,0,0,1,0,0,0,0,1,0,0,1,1,1,1,0],
    [0,1,0,0,1,0,0,1,0,0,0,0,1,0,0,1,0,0,1,0],
    [0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0],
    [0,1,0,1,0,0,1,0,0,1,1,0,0,1,0,0,1,0,1,0],
    [0,2,0,1,0,0,1,0,0,1,1,0,0,1,0,0,1,0,2,0],
    [0,1,0,1,0,0,1,0,0,1,1,0,0,1,0,0,1,0,1,0],
    [0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0],
    [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],
]

def main(stdscr, speed):
    curses.curs_set(0)
    stdscr.nodelay(1)
    stdscr.timeout(0)
    curses.start_color()
    curses.use_default_colors()
    curses.init_pair(1, curses.COLOR_YELLOW, -1)   # pacman
    curses.init_pair(2, curses.COLOR_RED, -1)     # ghost
    curses.init_pair(3, curses.COLOR_GREEN, -1)   # points
    curses.init_pair(4, curses.COLOR_CYAN, -1)    # super points
    curses.init_pair(5, curses.COLOR_WHITE, -1)   # walls
    curses.init_pair(6, curses.COLOR_BLUE, -1)    # scared ghost

    height, width = stdscr.getmaxyx()
    if height < 22 or width < 42:
        print("Терминал слишком мал (нужно 22x42)")
        return

    H, W = 20, 20
    map_data = [row[:] for row in MAP]
    pac_x, pac_y = 9, 9
    pac_dir = (0, 0)
    score = 0
    lives = 3
    level = 1
    best = load_record()
    game_over = False
    frame = speed / 1000.0

    ghosts = [
        {'x': 9, 'y': 8, 'color': 2, 'dir': (1,0), 'scared': False, 'home': (9,8)},
        {'x': 8, 'y': 9, 'color': 2, 'dir': (0,1), 'scared': False, 'home': (8,9)},
        {'x': 10, 'y': 9, 'color': 2, 'dir': (0,-1), 'scared': False, 'home': (10,9)},
        {'x': 9, 'y': 10, 'color': 2, 'dir': (-1,0), 'scared': False, 'home': (9,10)},
    ]
    scared_timer = 0
    total_dots = sum(row.count(1) + row.count(2) for row in map_data)

    def draw():
        stdscr.clear()
        for y in range(H):
            for x in range(W):
                ch = map_data[y][x]
                if ch == 0:
                    stdscr.addch(y+1, x*2+1, '#', curses.color_pair(5))
                elif ch == 1:
                    stdscr.addch(y+1, x*2+1, '.', curses.color_pair(3))
                elif ch == 2:
                    stdscr.addch(y+1, x*2+1, 'O', curses.color_pair(4))
        # Pac-Man
        stdscr.addch(pac_y+1, pac_x*2+1, 'C', curses.color_pair(1) | curses.A_BOLD)
        # Ghosts
        for g in ghosts:
            color = curses.color_pair(6) if g['scared'] else curses.color_pair(g['color'])
            stdscr.addch(g['y']+1, g['x']*2+1, 'G', color | curses.A_BOLD)
        # Score
        stdscr.addstr(0, 2, f"Score: {score}", curses.color_pair(5))
        stdscr.addstr(0, W*2-12, f"Best: {best}", curses.color_pair(5))
        stdscr.addstr(0, W*2-25, f"Lives: {lives}  Level: {level}", curses.color_pair(5))
        if game_over:
            msg = "GAME OVER! Press R to restart, Q to quit"
            stdscr.addstr(H//2+1, (W*2 - len(msg))//2, msg, curses.color_pair(1))
        stdscr.refresh()

    while True:
        key = stdscr.getch()
        if key == ord('q') or key == ord('Q'): break
        if key == ord('r') or key == ord('R'):
            if game_over:
                map_data = [row[:] for row in MAP]
                pac_x, pac_y = 9, 9
                pac_dir = (0,0)
                score = 0
                lives = 3
                level = 1
                game_over = False
                scared_timer = 0
                for g in ghosts:
                    g['x'], g['y'] = g['home']
                    g['dir'] = (0,0)
                    g['scared'] = False
                continue

        if game_over:
            draw()
            continue

        # Управление
        if key == curses.KEY_LEFT or key == ord('a'): pac_dir = (0, -1)
        elif key == curses.KEY_RIGHT or key == ord('d'): pac_dir = (0, 1)
        elif key == curses.KEY_UP or key == ord('w'): pac_dir = (-1, 0)
        elif key == curses.KEY_DOWN or key == ord('s'): pac_dir = (1, 0)

        # Движение Pac-Man
        ny, nx = pac_y + pac_dir[0], pac_x + pac_dir[1]
        if 0 <= ny < H and 0 <= nx < W and map_data[ny][nx] != 0:
            pac_y, pac_x = ny, nx
            # Сбор точек
            if map_data[ny][nx] == 1:
                score += 10
                map_data[ny][nx] = 3
                total_dots -= 1
            elif map_data[ny][nx] == 2:
                score += 50
                map_data[ny][nx] = 3
                total_dots -= 1
                scared_timer = 30  # кадров испуга
                for g in ghosts:
                    g['scared'] = True
                    g['dir'] = (random.choice([-1,0,1]), random.choice([-1,0,1]))

        # Движение призраков
        for g in ghosts:
            if g['scared']:
                # Убегаем от Pac-Man
                dx = g['x'] - pac_x
                dy = g['y'] - pac_y
                # Случайное движение с уклоном
                choices = [(1,0), (-1,0), (0,1), (0,-1)]
                if abs(dx) > abs(dy):
                    choices.sort(key=lambda d: abs(d[0]) if d[0]!=0 else 0, reverse=True)
                else:
                    choices.sort(key=lambda d: abs(d[1]) if d[1]!=0 else 0, reverse=True)
                for d in choices:
                    nx2 = g['x'] + d[0]
                    ny2 = g['y'] + d[1]
                    if 0 <= nx2 < W and 0 <= ny2 < H and map_data[ny2][nx2] != 0:
                        g['x'], g['y'] = nx2, ny2
                        break
            else:
                # Простое случайное движение
                dirs = [(1,0), (-1,0), (0,1), (0,-1)]
                random.shuffle(dirs)
                for d in dirs:
                    nx2 = g['x'] + d[0]
                    ny2 = g['y'] + d[1]
                    if 0 <= nx2 < W and 0 <= ny2 < H and map_data[ny2][nx2] != 0:
                        g['x'], g['y'] = nx2, ny2
                        break

        # Проверка столкновений с призраками
        for g in ghosts:
            if g['x'] == pac_x and g['y'] == pac_y:
                if g['scared']:
                    # Съедаем призрака
                    score += 100
                    g['x'], g['y'] = g['home']
                    g['scared'] = False
                    g['dir'] = (0,0)
                else:
                    lives -= 1
                    if lives <= 0:
                        game_over = True
                        if score > best:
                            best = score
                            save_record(best)
                    else:
                        # Респавн
                        pac_x, pac_y = 9, 9
                        for g2 in ghosts:
                            g2['x'], g2['y'] = g2['home']
                            g2['scared'] = False
                break

        if scared_timer > 0:
            scared_timer -= 1
            if scared_timer == 0:
                for g in ghosts:
                    g['scared'] = False

        # Проверка завершения уровня
        if total_dots == 0:
            level += 1
            map_data = [row[:] for row in MAP]
            total_dots = sum(row.count(1) + row.count(2) for row in map_data)
            pac_x, pac_y = 9, 9
            for g in ghosts:
                g['x'], g['y'] = g['home']
                g['scared'] = False
                g['dir'] = (0,0)

        draw()
        time.sleep(frame)

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('-s', '--speed', type=int, default=150, help='Скорость (мс)')
    args = parser.parse_args()
    try:
        curses.wrapper(main, args.speed)
    except KeyboardInterrupt:
        print("\nИгра завершена.")
