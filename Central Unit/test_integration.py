import unittest
import json
from centralunit import create_app, db
from models import PhoneData, SimulatedData, CombinedData, TemporaryData

class TestIntegration(unittest.TestCase):

    def setUp(self):
        self.app = create_app()
        self.app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:///:memory:'
        self.app.config['TESTING'] = True
        self.client = self.app.test_client()

        with self.app.app_context():
            db.create_all()

            # Add initial data to the database for testing
            phone_data = PhoneData(x=1, y=2, z=3, rssi_HUAWEI_CUTE_BD57=-50, rssi_YaSeR_Osama=-60, rssi_MG2024=-70, rssi_Samas_iPhone=-80, mag_x=0.1, mag_y=0.2, mag_z=0.3)
            db.session.add(phone_data)
            db.session.commit()

    def tearDown(self):
        with self.app.app_context():
            db.session.remove()
            db.drop_all()

    def test_receive_phone_data(self):
        data = {
            'x': 10,
            'y': 20,
            'z': 30,
            'rssi_HUAWEI_CUTE_BD57': -50,
            'rssi_YaSeR_Osama': -60,
            'rssi_MG2024': -70,
            'rssi_Samas_iPhone': -80,
            'mag_x': 1.1,
            'mag_y': 1.2,
            'mag_z': 1.3
        }
        response = self.client.post('/phone_data', data=json.dumps(data), content_type='application/json')
        self.assertEqual(response.status_code, 200)
        self.assertIn('Phone data received successfully!', response.json['message'])

    def test_get_phone_data(self):
        response = self.client.get('/phone_data')
        self.assertEqual(response.status_code, 200)
        self.assertTrue(len(response.json) > 0)

    def test_shortest_path(self):
        data = {
            'start': [1, 2],
            'goal': [2, 3]
        }
        response = self.client.post('/shortest_path', data=json.dumps(data), content_type='application/json')
        self.assertEqual(response.status_code, 200)
        self.assertIn('path', response.json)

    def test_receive_simulated_data(self):
        data = {
            'meanToA': 0.1,
            'stdToA': 0.2,
            'maxAmplitude': 1.0,
            'meanAmplitude': 0.8,
            'stdAmplitude': 0.1,
            'rssi_HUAWEI_CUTE_BD57': -50,
            'rssi_YaSeR_Osama': -60,
            'rssi_MG2024': -70,
            'rssi_Samas_iPhone': -80,
            'x': 10,
            'y': 20,
            'z': 30
        }
        response = self.client.post('/simulated_data', data=json.dumps(data), content_type='application/json')
        self.assertEqual(response.status_code, 200)
        self.assertIn('Simulated data received successfully!', response.json['message'])

    def test_view_combined_data(self):
        response = self.client.get('/view_combined_data')
        self.assertEqual(response.status_code, 200)
        self.assertTrue(len(response.json) > 0)

if __name__ == '__main__':
    unittest.main()
