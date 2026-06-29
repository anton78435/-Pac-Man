#!/usr/bin/env ruby
# pacman.rb
# encoding: UTF-8

require 'curses'
require 'json'
require 'fileutils'

RECORD_FILE = File.join(Dir.home, '.pacman_record.json')
def load_record
  return 0 unless File.exist?(RECORD_FILE)
  JSON.parse(File.read(RECORD_FILE))['record'] || 0
rescue
  0
end
def save_record(record)
  File.write(RECORD_FILE, JSON.pretty_generate(record: record))
end

Curses.init_screen
Curses.start_color
Curses.use_default_colors
Curses.init_pair(1, Curses::COLOR_YELLOW, -1)
Curses.init_pair(2, Curses::COLOR_RED, -1)
Curses.init_pair(3, Curses::COLOR_GREEN, -1)
Curses.init_pair(4, Curses::COLOR_CYAN, -1)
Curses.init_pair(5, Curses::COLOR_WHITE, -1)
Curses.init_pair(6, Curses::COLOR_BLUE, -1)

height = Curses.lines
width = Curses.cols
if height < 22 || width < 42
  puts "Terminal too small"
  exit 1
end

speed = 150
if ARGV.include?('-s') && ARGV.index('-s') + 1 < ARGV.size
  speed = ARGV[ARGV.index('-s') + 1].to_i
end

H=20; W=20
mapData = [
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
pacX=9; pacY=9; dirX=0; dirY=0
score=0; lives=3; level=1
best=load_record
gameOver=false
frame = speed/1000.0

ghosts = [
  {x:9, y:8, hx:9, hy:8, scared:false},
  {x:8, y:9, hx:8, hy:9, scared:false},
  {x:10, y:9, hx:10, hy:9, scared:false},
  {x:9, y:10, hx:9, hy:10, scared:false},
]
scaredTimer=0
totalDots = mapData.flatten.count {|v| v==1 || v==2}

Curses.curs_set(0)
Curses.noecho
Curses.timeout=0

loop do
  ch = Curses.getch
  if ch == 'q' || ch == 'Q'
    break
  elsif ch == 'r' || ch == 'R'
    if gameOver
      mapData = [
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
      pacX=9; pacY=9; dirX=0; dirY=0
      score=0; lives=3; level=1; gameOver=false; scaredTimer=0
      ghosts.each do |g|
        g[:x]=g[:hx]; g[:y]=g[:hy]; g[:scared]=false
      end
      totalDots = mapData.flatten.count {|v| v==1 || v==2}
      next
    end
  end

  if ch == Curses::KEY_LEFT || ch == 'a'
    dirX=0; dirY=-1
  elsif ch == Curses::KEY_RIGHT || ch == 'd'
    dirX=0; dirY=1
  elsif ch == Curses::KEY_UP || ch == 'w'
    dirX=-1; dirY=0
  elsif ch == Curses::KEY_DOWN || ch == 's'
    dirX=1; dirY=0
  end

  if gameOver
    Curses.clear
    msg = "GAME OVER! Score: #{score}  Best: #{best}"
    Curses.setpos(height/2-2, (width - msg.length)/2)
    Curses.attron(Curses.color_pair(5)) { Curses.addstr(msg) }
    Curses.setpos(height/2, (width-20)/2)
    Curses.attron(Curses.color_pair(5)) { Curses.addstr("R - restart | Q - quit") }
    Curses.refresh
    next
  end

  # Move Pac-Man
  ny = pacY + dirX
  nx = pacX + dirY
  if ny>=0 && ny<H && nx>=0 && nx<W && mapData[ny][nx] != 0
    pacY=ny; pacX=nx
    if mapData[ny][nx] == 1
      score += 10
      mapData[ny][nx] = 3
      totalDots -= 1
    elsif mapData[ny][nx] == 2
      score += 50
      mapData[ny][nx] = 3
      totalDots -= 1
      scaredTimer = 30
      ghosts.each { |g| g[:scared] = true }
    end
  end

  # Ghosts movement
  dirs = [[1,0],[-1,0],[0,1],[0,-1]]
  ghosts.each do |g|
    dx, dy = dirs.sample
    nx2 = g[:x] + dx
    ny2 = g[:y] + dy
    if nx2>=0 && nx2<W && ny2>=0 && ny2<H && mapData[ny2][nx2] != 0
      g[:x] = nx2
      g[:y] = ny2
    end
  end

  # Collisions
  ghosts.each do |g|
    if g[:x] == pacX && g[:y] == pacY
      if g[:scared]
        score += 100
        g[:x] = g[:hx]
        g[:y] = g[:hy]
        g[:scared] = false
      else
        lives -= 1
        if lives <= 0
          gameOver = true
          if score > best
            best = score
            save_record(best)
          end
        else
          pacX=9; pacY=9
          ghosts.each do |g2|
            g2[:x]=g2[:hx]; g2[:y]=g2[:hy]; g2[:scared]=false
          end
        end
      end
      break
    end
  end

  if scaredTimer > 0
    scaredTimer -= 1
    if scaredTimer == 0
      ghosts.each { |g| g[:scared] = false }
    end
  end

  if totalDots == 0
    level += 1
    mapData = [
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
    totalDots = mapData.flatten.count {|v| v==1 || v==2}
    pacX=9; pacY=9
    ghosts.each do |g|
      g[:x]=g[:hx]; g[:y]=g[:hy]; g[:scared]=false
    end
  end

  # Draw
  Curses.clear
  (0...H).each do |y|
    (0...W).each do |x|
      v = mapData[y][x]
      if v == 0
        Curses.setpos(y+1, x*2+1)
        Curses.attron(Curses.color_pair(5)) { Curses.addstr('#') }
      elsif v == 1
        Curses.setpos(y+1, x*2+1)
        Curses.attron(Curses.color_pair(3)) { Curses.addstr('.') }
      elsif v == 2
        Curses.setpos(y+1, x*2+1)
        Curses.attron(Curses.color_pair(4)) { Curses.addstr('O') }
      end
    end
  end
  Curses.setpos(pacY+1, pacX*2+1)
  Curses.attron(Curses.color_pair(1)|Curses::A_BOLD) { Curses.addstr('C') }
  ghosts.each do |g|
    color = g[:scared] ? 6 : 2
    Curses.setpos(g[:y]+1, g[:x]*2+1)
    Curses.attron(Curses.color_pair(color)|Curses::A_BOLD) { Curses.addstr('G') }
  end
  Curses.setpos(0, 2)
  Curses.attron(Curses.color_pair(5)) { Curses.addstr("Score: #{score}") }
  Curses.setpos(0, W*2-12)
  Curses.attron(Curses.color_pair(5)) { Curses.addstr("Best: #{best}") }
  Curses.setpos(0, W*2-25)
  Curses.attron(Curses.color_pair(5)) { Curses.addstr("Lives: #{lives}  Level: #{level}") }
  if gameOver
    msg = "GAME OVER! Press R to restart, Q to quit"
    Curses.setpos(H/2+1, (W*2 - msg.length)/2)
    Curses.attron(Curses.color_pair(5)) { Curses.addstr(msg) }
  end
  Curses.refresh
  sleep(frame)
end

Curses.close_screen
