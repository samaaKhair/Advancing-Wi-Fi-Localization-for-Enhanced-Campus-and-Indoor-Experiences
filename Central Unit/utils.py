import numpy as np
import heapq

def heuristic(a, b):
    return np.linalg.norm(np.array(a) - np.array(b))

def astar(grid, start, goal):
    neighbors = [(0, 1), (0, -1), (1, 0), (-1, 0), (1, 1), (1, -1), (-1, 1), (-1, -1)]
    close_set = set()
    came_from = {}
    gscore = {start: 0}
    fscore = {start: heuristic(start, goal)}
    oheap = []

    heapq.heappush(oheap, (fscore[start], start))

    while oheap:
        current = heapq.heappop(oheap)[1]

        if current == goal:
            path = []
            while current in came_from:
                path.append(current)
                current = came_from[current]
            path.append(start)
            return path[::-1]

        close_set.add(current)
        for i, j in neighbors:
            neighbor = current[0] + i, current[1] + j
            tentative_g_score = gscore[current] + heuristic(current, neighbor)
            if 0 <= neighbor[0] < grid.shape[0]:
                if 0 <= neighbor[1] < grid.shape[1]:
                    if grid[neighbor[0]][neighbor[1]] == 1:
                        continue
                else:
                    continue
            else:
                continue

            if neighbor in close_set and tentative_g_score >= gscore.get(neighbor, 0):
                continue

            if tentative_g_score < gscore.get(neighbor, 0) or neighbor not in [i[1] for i in oheap]:
                came_from[neighbor] = current
                gscore[neighbor] = tentative_g_score
                fscore[neighbor] = tentative_g_score + heuristic(neighbor, goal)
                heapq.heappush(oheap, (fscore[neighbor], neighbor))

    return False

# def heuristic(a, b):
#     return np.linalg.norm(np.array(a) - np.array(b))

# def astar(grid, start, goal):
#     neighbors = [(0, 1), (0, -1), (1, 0), (-1, 0), (1, 1), (1, -1), (-1, 1), (-1, -1)]
#     close_set = set()
#     came_from = {}
#     gscore = {start: 0}
#     fscore = {start: heuristic(start, goal)}
#     oheap = []

#     heapq.heappush(oheap, (fscore[start], start))

#     while oheap:
#         current = heapq.heappop(oheap)[1]

#         if current == goal:
#             path = []
#             while current in came_from:
#                 path.append(current)
#                 current = came_from[current]
#             path.append(start)
#             return path[::-1]

#         close_set.add(current)
#         for i, j in neighbors:
#             neighbor = current[0] + i, current[1] + j
#             tentative_g_score = gscore[current] + heuristic(current, neighbor)
#             if 0 <= neighbor[0] < grid.shape[0]:
#                 if 0 <= neighbor[1] < grid.shape[1]:
#                     if grid[neighbor[0]][neighbor[1]] == 1:
#                         continue
#                 else:
#                     continue
#             else:
#                 continue

#             if neighbor in close_set and tentative_g_score >= gscore.get(neighbor, 0):
#                 continue

#             if tentative_g_score < gscore.get(neighbor, 0) or neighbor not in [i[1] for i in oheap]:
#                 came_from[neighbor] = current
#                 gscore[neighbor] = tentative_g_score
#                 fscore[neighbor] = tentative_g_score + heuristic(neighbor, goal)
#                 heapq.heappush(oheap, (fscore[neighbor], neighbor))

#     return False