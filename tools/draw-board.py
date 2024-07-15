#!/usr/bin/env python3

from math import *

sides = 5

# The board consists of vertices that lie on concentric pentagons,
# such that the pentagon of size S has S vertices per side.
#
# Each pentagon has 5 sides, of course, so a vertex can be identified
# by the triple (size, side, index) where size is the size of the pentagon,
# side is the side of the pentagon (0 <= side < 5) and index is the index
# of the pentago (0 <= index < size).
#
# To make the board look more aesthetically pleasing, we calculate not just the
# position of vertices on a regular pentagon, but also on a circle with radius
# `size` that has 5*size vertices on it, and then we take a weighted average
# using the parameter `roundness` (0 <= roundness <= 1). The board shouldn't
# be perfectly round, because then the edges of the board are unclear!
#
def VertexPos(size, side, index, scale=1, roundness=0.25):
  assert 0 <= size
  assert 0 <= side < sides
  assert 0 <= index <= size
  if size == 0: return (0, 0)
  # (x1, y1) is the first endpoint of the side of the polygon
  x1 = cos(side/sides*2*pi - pi/2)
  y1 = sin(side/sides*2*pi - pi/2)
  # (x2, y2) is the second endpoint of the side of the polygon
  x2 = cos((side + 1)/sides*2*pi - pi/2)
  y2 = sin((side + 1)/sides*2*pi - pi/2)
  # (x3, y3) is the point on side of the polygon corresponding to `index`
  x3 = ((size - index)*x1 + index*x2)
  y3 = ((size - index)*y1 + index*y2)
  # (x4, y4) is the point on a circle with 5*size points spread around evenly.
  x4 = size*cos((side*size + index)/(sides*size)*2*pi - pi/2)
  y4 = size*sin((side*size + index)/(sides*size)*2*pi - pi/2)
  # the final point (x, y) is calculated as a weighted average of (x3, y3) and
  # (x4, y4)
  x = (1 - roundness)*x3 + roundness*x4
  y = (1 - roundness)*y3 + roundness*y4
  return (scale*x, scale*y)

# We can convert (size, side index) triples to consecutive integers, starting
# from the center of the board, so that:
#
#  vertex 0 is the origin
#  vertices 1..5 lie on the first pentagon (1 per side)
#  vertices 6..15 lie on on the second pentagon (2 per side)
#  etc.
#
def VertexToId(size, side, index):
  assert 0 <= size
  assert 0 <= side < sides
  assert 0 <= index <= size
  if size == 0:
    return 0  # origin
  if index == size:
    index = 0
    side = (side + 1) % sides
  return 1 + sides*(size - 1)*size//2 + size*side + index

# IdToVertex(i) is the inverse of VertexToId().
def IdToVertex(i):
  assert 0 <= i
  if i == 0:
    return (0, 0, 0)  # origin
  i -= 1
  size = 1
  while i >= sides*size:
    i -= sides*size
    size += 1
  side = i // size
  index = i % size
  return (size, side, index)

# Returns the total number of vertices on a board with the given size.
#
#  VertexCount(0) = 1
#  VertexCount(1) = 6
#  VertexCount(2) = 16
#  etc.
def VertexCount(board_size):
  return VertexToId(board_size, 0, 0)

# Returns the total number of edges on a board with the given size.
#
# Equal to the length of the edge list returned by GenerateEdges(board_size).
#
#  EdgeCount(0) = 0
#  EdgeCount(1) = 10
#  EdgeCount(2) = 35
#  EdgeCount(3) = 75
#  etc.

def EdgeCount(board_size):
  return sides*(board_size - 1)*(3*board_size - 2)//2

# Generates edges for the board of the given size, as a list of vertex ids.
def GenerateEdges(board_size):
  edges = []
  for size in range(1, board_size):
    for side in range(sides):
      for index in range(size):
        v = VertexToId(size, side, index)
        assert IdToVertex(v) == (size, side, index)
        edges.append((v, VertexToId(size, side, index + 1)))
        edges.append((VertexToId(size - 1, side, index), v))
        if index > 0: edges.append((VertexToId(size - 1, side, index - 1), v))
  return edges

# Generate CodeCup ids by starting at the top, then enumerating a row at a time,
# from left to right:
def GenerateCodeCupIds(board_size):
  vertex_count = VertexCount(board_size)
  edges = GenerateEdges(board_size)
  adj = [[] for _ in range(vertex_count)]
  for v, w in edges:
    adj[v].append(w)
    adj[w].append(v)
  codecup_ids = [0]*vertex_count
  todo = [VertexToId(board_size - 1, 0, 0)]
  seen = set(todo)
  next_id = 1
  while todo:
    next_todo = []
    for v in todo:
      codecup_ids[v] = next_id
      next_id += 1
      for w in adj[v]:
        if w not in seen:
          seen.add(w)
          next_todo.append(w)
    next_todo.sort(key=lambda v: VertexPos(*IdToVertex(v))[0])
    todo = next_todo
  return codecup_ids


def WriteBoard(filename, board_size=7, scale=15, label=None):

  if label == 'codecup-id':
    codecup_ids = GenerateCodeCupIds(board_size)

  with open(filename, 'wt') as f:
    height = width = (board_size + 0.5)*scale*2
    print(f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="{-width/2} {-height/2} {width} {height}">', file=f)
    for (i, j) in GenerateEdges(board_size):
      size1, side1, index1 = IdToVertex(i)
      size2, side2, index2 = IdToVertex(j)
      (x1, y1) = VertexPos(size1, side1, index1, scale)
      (x2, y2) = VertexPos(size2, side2, index2, scale)
      print(f'<path d="M {x1} {y1} L {x2} {y2}" stroke="black" fill="none" stroke-width="1" stroke-linecap="round"/>', file=f)

    for i in range(VertexCount(board_size)):
      size, side, index = IdToVertex(i)
      x, y = VertexPos(size, side, index, scale)
      print(f'<circle cx="{x}" cy="{y}" r="{0.2*scale}" fill="black"/>', file=f)
      if label == 'id':
        print(f'<text x="{x}" y="{y}" font-size="{0.2*scale}" font-family="sans-serif" fill="white" text-anchor="middle" dominant-baseline="middle">{i}</text>', file=f)
      elif label == 'codecup-id':
        print(f'<text x="{x}" y="{y}" font-size="{0.2*scale}" font-family="sans-serif" fill="white" text-anchor="middle" dominant-baseline="middle">{codecup_ids[i]}</text>', file=f)
      elif label == 'coords':
        print(f'<text x="{x}" y="{y}" font-size="{0.15*scale}" font-family="sans-serif" fill="white" text-anchor="middle" dominant-baseline="middle">{size},{side},{index}</text>', file=f)
      else:
        assert label is None

    print('</svg>', file=f)

if __name__ == '__main__':
  # WriteBoard('board-7.svg')
  # WriteBoard('board-7-id.svg', label='id')
  # WriteBoard('board-7-coords.svg', label='coords')
  # WriteBoard('board-7-codecup-id.svg', label='codecup-id')
  WriteBoard('board-5.svg', board_size=5)
