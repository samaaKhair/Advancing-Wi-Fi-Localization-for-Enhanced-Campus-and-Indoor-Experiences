# test_utils.py
import unittest
import numpy as np
from utils import astar, heuristic

class TestAStarAlgorithm(unittest.TestCase):
    def setUp(self):
        self.grid = np.zeros((10, 10))
        self.grid[1, 7] = 1  # Obstacle
        self.start = (0, 0)
        self.goal = (9, 9)

    def test_heuristic(self):
        self.assertEqual(heuristic((0, 0), (1, 1)), np.sqrt(2))

    def test_astar_find_path(self):
        path = astar(self.grid, self.start, self.goal)
        self.assertIsNotNone(path)
        self.assertEqual(path[0], self.start)
        self.assertEqual(path[-1], self.goal)

    def test_astar_no_path(self):
        self.grid[1, 0:10] = 1  # Create an obstacle blocking the path
        path = astar(self.grid, self.start, self.goal)
        self.assertFalse(path)

if __name__ == '__main__':
    unittest.main()
