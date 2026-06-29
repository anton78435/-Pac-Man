// pacman.go
package main

import (
	"encoding/json"
	"fmt"
	"math/rand"
	"os"
	"time"
	"github.com/nsf/termbox-go"
)

const recordFile = ".pacman_record.json"
type Record struct{ Best int }

func loadRecord() int {
	f, err := os.Open(recordFile)
	if err != nil { return 0 }
	defer f.Close()
	var r Record
	json.NewDecoder(f).Decode(&r)
	return r.Best
}
func saveRecord(best int) {
	f, _ := os.Create(recordFile)
	defer f.Close()
	json.NewEncoder(f).Encode(Record{best})
}

func main() {
	speed := 150
	if len(os.Args) > 2 && os.Args[1] == "-s" {
		if s, err := strconv.Atoi(os.Args[2]); err == nil && s > 0 { speed = s }
	}
	err := termbox.Init()
	if err != nil { fmt.Println(err); return }
	defer termbox.Close()
	termbox.SetInputMode(termbox.InputEsc)
	w, h := termbox.Size()
	if h < 22 || w < 42 { fmt.Println("Terminal too small"); return }
	rand.Seed(time.Now().UnixNano())

	const H, W = 20, 20
	mapData := [][]int{
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
	}
	pacX, pacY := 9, 9
	dirX, dirY := 0, 0
	score, lives, level := 0, 3, 1
	best := loadRecord()
	gameOver := false
	frame := time.Duration(speed) * time.Millisecond

	type Ghost struct{ x, y, color, hx, hy int; scared bool }
	ghosts := []Ghost{
		{9,8,2,9,8,false},
		{8,9,2,8,9,false},
		{10,9,2,10,9,false},
		{9,10,2,9,10,false},
	}
	scaredTimer := 0
	totalDots := func() int {
		c := 0
		for _, row := range mapData {
			for _, v := range row {
				if v == 1 || v == 2 { c++ }
			}
		}
		return c
	}
	total := totalDots()

	tbprint := func(x, y int, fg, bg termbox.Attribute, msg string) {
		for _, ch := range msg {
			termbox.SetCell(x, y, ch, fg, bg)
			x++
		}
	}

	for {
		ev := termbox.PollEvent()
		if ev.Type == termbox.EventKey {
			if ev.Key == termbox.KeyEsc || ev.Ch == 'q' { return }
			if ev.Ch == 'r' && gameOver {
				// reset
				mapData = [][]int{
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
				}
				pacX, pacY = 9, 9
				dirX, dirY = 0, 0
				score, lives, level = 0, 3, 1
				gameOver = false
				scaredTimer = 0
				for i := range ghosts {
					ghosts[i].x = ghosts[i].hx
					ghosts[i].y = ghosts[i].hy
					ghosts[i].scared = false
				}
				total = totalDots()
				continue
			}
			if ev.Key == termbox.KeyArrowLeft || ev.Ch == 'a' { dirX, dirY = 0, -1 }
			if ev.Key == termbox.KeyArrowRight || ev.Ch == 'd' { dirX, dirY = 0, 1 }
			if ev.Key == termbox.KeyArrowUp || ev.Ch == 'w' { dirX, dirY = -1, 0 }
			if ev.Key == termbox.KeyArrowDown || ev.Ch == 's' { dirX, dirY = 1, 0 }
		}

		if gameOver {
			termbox.Clear(termbox.ColorDefault, termbox.ColorDefault)
			msg := fmt.Sprintf("GAME OVER! Score: %d  Best: %d", score, best)
			tbprint(w/2-len(msg)/2, h/2-2, termbox.ColorWhite, termbox.ColorDefault, msg)
			tbprint(w/2-15, h/2, termbox.ColorCyan, termbox.ColorDefault, "R - restart | Q - quit")
			termbox.Flush()
			continue
		}

		// Move Pac-Man
		ny, nx := pacY+dirX, pacX+dirY
		if ny >= 0 && ny < H && nx >= 0 && nx < W && mapData[ny][nx] != 0 {
			pacY, pacX = ny, nx
			if mapData[ny][nx] == 1 { score += 10; mapData[ny][nx] = 3; total-- }
			if mapData[ny][nx] == 2 { score += 50; mapData[ny][nx] = 3; total--; scaredTimer = 30; for i := range ghosts { ghosts[i].scared = true } }
		}

		// Move ghosts
		for i := range ghosts {
			g := &ghosts[i]
			dirs := [][2]int{{1,0},{-1,0},{0,1},{0,-1}}
			if g.scared {
				// убегаем – случайно
				idx := rand.Intn(len(dirs))
				dx, dy := dirs[idx][0], dirs[idx][1]
				nx2, ny2 := g.x+dx, g.y+dy
				if nx2 >= 0 && nx2 < W && ny2 >= 0 && ny2 < H && mapData[ny2][nx2] != 0 {
					g.x, g.y = nx2, ny2
				}
			} else {
				idx := rand.Intn(len(dirs))
				dx, dy := dirs[idx][0], dirs[idx][1]
				nx2, ny2 := g.x+dx, g.y+dy
				if nx2 >= 0 && nx2 < W && ny2 >= 0 && ny2 < H && mapData[ny2][nx2] != 0 {
					g.x, g.y = nx2, ny2
				}
			}
		}

		// Collisions
		for i := range ghosts {
			g := &ghosts[i]
			if g.x == pacX && g.y == pacY {
				if g.scared {
					score += 100
					g.x, g.y = g.hx, g.hy
					g.scared = false
				} else {
					lives--
					if lives <= 0 {
						gameOver = true
						if score > best { best = score; saveRecord(best) }
					} else {
						pacX, pacY = 9, 9
						for j := range ghosts {
							ghosts[j].x = ghosts[j].hx
							ghosts[j].y = ghosts[j].hy
							ghosts[j].scared = false
						}
					}
				}
				break
			}
		}

		if scaredTimer > 0 {
			scaredTimer--
			if scaredTimer == 0 {
				for i := range ghosts {
					ghosts[i].scared = false
				}
			}
		}

		if total == 0 {
			level++
			// reset map
			mapData = [][]int{
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
			}
			total = totalDots()
			pacX, pacY = 9, 9
			for i := range ghosts {
				ghosts[i].x = ghosts[i].hx
				ghosts[i].y = ghosts[i].hy
				ghosts[i].scared = false
			}
		}

		// Draw
		termbox.Clear(termbox.ColorDefault, termbox.ColorDefault)
		for y := 0; y < H; y++ {
			for x := 0; x < W; x++ {
				v := mapData[y][x]
				if v == 0 {
					termbox.SetCell(x*2+1, y+1, '#', termbox.ColorWhite, termbox.ColorDefault)
				} else if v == 1 {
					termbox.SetCell(x*2+1, y+1, '.', termbox.ColorGreen, termbox.ColorDefault)
				} else if v == 2 {
					termbox.SetCell(x*2+1, y+1, 'O', termbox.ColorCyan, termbox.ColorDefault)
				}
			}
		}
		termbox.SetCell(pacX*2+1, pacY+1, 'C', termbox.ColorYellow|termbox.AttrBold, termbox.ColorDefault)
		for _, g := range ghosts {
			color := termbox.ColorBlue
			if !g.scared {
				color = termbox.ColorRed
			}
			termbox.SetCell(g.x*2+1, g.y+1, 'G', color|termbox.AttrBold, termbox.ColorDefault)
		}
		tbprint(2, 0, termbox.ColorWhite, termbox.ColorDefault, fmt.Sprintf("Score: %d", score))
		tbprint(W*2-12, 0, termbox.ColorWhite, termbox.ColorDefault, fmt.Sprintf("Best: %d", best))
		tbprint(W*2-25, 0, termbox.ColorWhite, termbox.ColorDefault, fmt.Sprintf("Lives: %d  Level: %d", lives, level))
		if gameOver {
			msg := "GAME OVER! Press R to restart, Q to quit"
			tbprint((W*2-len(msg))/2, H/2+1, termbox.ColorWhite, termbox.ColorDefault, msg)
		}
		termbox.Flush()
		time.Sleep(frame)
	}
}
