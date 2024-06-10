import numpy as np
import heapq
from flask import Flask, request, jsonify
from models import db, SimulatedData, PhoneData, CombinedData, TemporaryData
import joblib

app = Flask(__name__)
app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:///localization.db'
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
db.init_app(app)

with app.app_context():
    db.create_all()


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

@app.route('/shortest_path', methods=['POST'])
def shortest_path():
    data = request.get_json()
    start = tuple(data['start'])
    goal = tuple(data['goal'])

    # Query the PhoneData database to get the available coordinates
    phone_data = PhoneData.query.all()
    available_coords = {(int(d.x), int(d.y)) for d in phone_data}

    # Determine the grid size based on the available data
    if not available_coords:
        return jsonify({'message': 'No accessible coordinates found in the database'}), 404
    
    max_coord = max(max(x, y) for x, y in available_coords) + 1
    grid_size = max(max(max_coord, start[0], goal[0]), max(max_coord, start[1], goal[1]))

    # Create the grid and mark available coordinates
    grid = np.ones((grid_size, grid_size))  # Start with all cells unavailable (marked as 1)
    for x, y in available_coords:
        grid[x, y] = 0  # Mark available coordinates with 0
    grid[start[0], start[1]] = 0
    grid[goal[0], goal[1]] = 0

    path = astar(grid, start, goal)
    if path:
        return jsonify({'path': path}), 200
    else:
        return jsonify({'message': 'Path not found'}), 404


@app.route('/coordinates', methods=['GET'])
def get_coordinates():
    try:
        data = PhoneData.query.all()
        results = [
            {
                "row": d.x,  # Assuming x corresponds to the row
                "col": d.y,   # Assuming y corresponds to the column
                "height": d.z
            } for d in data
        ]
        return jsonify(results), 200
    except Exception as e:
        app.logger.error(f"Error retrieving phone data: {e}")
        return jsonify({'message': 'Error: Data retrieval failed'}), 500

@app.route('/phone_data', methods=['POST'])
def receive_phone_data():
    try:
        data = request.get_json()
        x = data.get('x')
        y = data.get('y')
        z = data.get('z')
        rssi_HUAWEI_CUTE_BD57 = data.get('rssi_HUAWEI_CUTE_BD57')
        rssi_YaSeR_Osama = data.get('rssi_YaSeR_Osama')
        rssi_MG2024 = data.get('rssi_MG2024')
        rssi_Samas_iPhone = data.get('rssi_Samas_iPhone')
        mag_x = data.get('mag_x')
        mag_y = data.get('mag_y')
        mag_z = data.get('mag_z')

        # Save the data to the database
        new_data = PhoneData(
            x=x, 
            y=y, 
            z=z, 
            rssi_HUAWEI_CUTE_BD57=rssi_HUAWEI_CUTE_BD57,
                        rssi_YaSeR_Osama=rssi_YaSeR_Osama, 
            rssi_MG2024=rssi_MG2024,
            rssi_Samas_iPhone=rssi_Samas_iPhone, 
            mag_x=mag_x, 
            mag_y=mag_y, 
            mag_z=mag_z
        )
        db.session.add(new_data)
        db.session.commit()

        # Update combined data
        new_combined_data = CombinedData(
            RSSI_TX_1=rssi_HUAWEI_CUTE_BD57,
            RSSI_TX_2=rssi_YaSeR_Osama,
            RSSI_TX_3=rssi_MG2024,
            RSSI_TX_4=rssi_Samas_iPhone
        )
        db.session.add(new_combined_data)
        db.session.commit()

        app.logger.info(f"Received phone data: {data}")

        return jsonify({'message': 'Phone data received successfully!'}), 200
    except Exception as e:
        app.logger.error(f"Error receiving phone data: {e}")
        return jsonify({'message': 'Error: Data processing failed'}), 500

@app.route('/phone_data', methods=['GET'])
def get_phone_data():
    try:
        data = PhoneData.query.all()
        results = [
            {
                "id": d.id,
                "x": d.x,
                "y": d.y,
                "z": d.z,
                "rssi_HUAWEI_CUTE_BD57": d.rssi_HUAWEI_CUTE_BD57,
                "rssi_YaSeR_Osama": d.rssi_YaSeR_Osama,
                "rssi_MG2024": d.rssi_MG2024,
                "rssi_Samas_iPhone": d.rssi_Samas_iPhone,
                "mag_x": d.mag_x,
                "mag_y": d.mag_y,
                "mag_z": d.mag_z
            } for d in data
        ]

        return jsonify(results), 200
    except Exception as e:
        app.logger.error(f"Error retrieving phone data: {e}")
        return jsonify({'message': 'Error: Data retrieval failed'}), 500

@app.route('/simulated_data', methods=['POST'])
def receive_simulated_data():
    try:
        data = request.get_json()
        meanToA_TX_1 = data.get('meanToA_TX_1')
        stdToA_TX_1 = data.get('stdToA_TX_1')
        maxAmplitude_TX_1 = data.get('maxAmplitude_TX_1')
        meanAmplitude_TX_1 = data.get('meanAmplitude_TX_1')
        stdAmplitude_TX_1 = data.get('stdAmplitude_TX_1')
        meanToA_TX_2 = data.get('meanToA_TX_2')
        stdToA_TX_2 = data.get('stdToA_TX_2')
        maxAmplitude_TX_2 = data.get('maxAmplitude_TX_2')
        meanAmplitude_TX_2 = data.get('meanAmplitude_TX_2')
        stdAmplitude_TX_2 = data.get('stdAmplitude_TX_2')
        meanToA_TX_3 = data.get('meanToA_TX_3')
        stdToA_TX_3 = data.get('stdToA_TX_3')
        maxAmplitude_TX_3 = data.get('maxAmplitude_TX_3')
        meanAmplitude_TX_3 = data.get('meanAmplitude_TX_3')
        stdAmplitude_TX_3 = data.get('stdAmplitude_TX_3')
        meanToA_TX_4 = data.get('meanToA_TX_4')
        stdToA_TX_4 = data.get('stdToA_TX_4')
        maxAmplitude_TX_4 = data.get('maxAmplitude_TX_4')
        meanAmplitude_TX_4 = data.get('meanAmplitude_TX_4')
        stdAmplitude_TX_4 = data.get('stdAmplitude_TX_4')
        RSSI_TX_1 = data.get('RSSI_TX_1')
        RSSI_TX_2 = data.get('RSSI_TX_2')
        RSSI_TX_3 = data.get('RSSI_TX_3')
        RSSI_TX_4 = data.get('RSSI_TX_4')
        x = data.get('x')
        y = data.get('y')
        z = data.get('z')

        # Save the data to the database
        new_data = SimulatedData(
            meanToA_TX_1=meanToA_TX_1, stdToA_TX_1=stdToA_TX_1, maxAmplitude_TX_1=maxAmplitude_TX_1,
            meanAmplitude_TX_1=meanAmplitude_TX_1, stdAmplitude_TX_1=stdAmplitude_TX_1,
            meanToA_TX_2=meanToA_TX_2, stdToA_TX_2=stdToA_TX_2, maxAmplitude_TX_2=maxAmplitude_TX_2,
            meanAmplitude_TX_2=meanAmplitude_TX_2, stdAmplitude_TX_2=stdAmplitude_TX_2,
            meanToA_TX_3=meanToA_TX_3, stdToA_TX_3=stdToA_TX_3, maxAmplitude_TX_3=maxAmplitude_TX_3,
            meanAmplitude_TX_3=meanAmplitude_TX_3, stdAmplitude_TX_3=stdAmplitude_TX_3,
            meanToA_TX_4=meanToA_TX_4, stdToA_TX_4=stdToA_TX_4, maxAmplitude_TX_4=maxAmplitude_TX_4,
            meanAmplitude_TX_4=meanAmplitude_TX_4, stdAmplitude_TX_4=stdAmplitude_TX_4,
            RSSI_TX_1=RSSI_TX_1, RSSI_TX_2=RSSI_TX_2, RSSI_TX_3=RSSI_TX_3, RSSI_TX_4=RSSI_TX_4,
            x=x, y=y, z=z
        )
        db.session.add(new_data)
        db.session.commit()

        # Update combined data
        new_combined_data = CombinedData(
            RSSI_TX_1=RSSI_TX_1,
            RSSI_TX_2=RSSI_TX_2,
            RSSI_TX_3=RSSI_TX_3,
            RSSI_TX_4=RSSI_TX_4
        )
        db.session.add(new_combined_data)
        db.session.commit()

        app.logger.info(f"Received simulated data: {data}")

        return jsonify({'message': 'Simulated data received successfully!'}), 200
    except Exception as e:
        app.logger.error(f"Error receiving simulated data: {e}")
        return jsonify({'message': 'Error: Data processing failed'}), 500

@app.route('/view_combined_data', methods=['GET'])
def view_combined_data():
    try:
        data = CombinedData.query.all()
        results = [
            {
                "id": d.id,
                "RSSI_TX_1": d.RSSI_TX_1,
                "RSSI_TX_2": d.RSSI_TX_2,
                "RSSI_TX_3": d.RSSI_TX_3,
                "RSSI_TX_4": d.RSSI_TX_4
            } for d in data
        ]
        return jsonify(results), 200
    except Exception as e:
        app.logger.error(f"Error retrieving combined data: {e}")
        return jsonify({'message': 'Error: Data retrieval failed'}), 500

@app.route('/update_combined_data/<int:id>', methods=['POST'])
def update_combined_data(id):
    try:
        data = CombinedData.query.get(id)
        if not data:
            return jsonify({'message': 'Data not found'}), 404

        data.RSSI_TX_1 = request.json.get('RSSI_TX_1', data.RSSI_TX_1)
        data.RSSI_TX_2 = request.json.get('RSSI_TX_2', data.RSSI_TX_2)
        data.RSSI_TX_3 = request.json.get('RSSI_TX_3', data.RSSI_TX_3)
        data.RSSI_TX_4 = request.json.get('RSSI_TX_4', data.RSSI_TX_4)

        db.session.commit()
        return jsonify({'message': 'Combined data updated successfully!'}), 200
    except Exception as e:
        app.logger.error(f"Error updating combined data: {e}")
        return jsonify({'message': 'Error: Data processing failed'}), 500

@app.route('/delete_combined_data/<int:id>', methods=['DELETE'])
def delete_combined_data(id):
    try:
        data = CombinedData.query.get(id)
        if not data:
            return jsonify({'message': 'Data not found'}), 404

        db.session.delete(data)
        db.session.commit()
        return jsonify({'message': 'Combined data deleted successfully!'}), 200
    except Exception as e:
        app.logger.error(f"Error deleting combined data: {e}")
        return jsonify({'message': 'Error: Data processing failed'}), 500
# Load the trained model
rf_model_real = joblib.load('rf_model_real.pkl')

def predict_location(data):
    # Extract RSSI values from the input data
    rssi_values = np.array([
        data.get('rssi_HUAWEI_CUTE_BD57'),
        data.get('rssi_YaSeR_Osama'),
        data.get('rssi_MG2024'),
        data.get('rssi_Samas_iPhone'),
        data.get('mag_x'),
        data.get('mag_y'),
        data.get('mag_z')

    ]).reshape(1, -1)
    
    # Use the model to predict the coordinates
    prediction = rf_model_real.predict(rssi_values)
    
    # Extract the predicted coordinates
    predicted_x, predicted_y, predicted_z = prediction[0]
    
    # Get predictions from all trees in the Random Forest
    all_tree_predictions = np.array([tree.predict(rssi_values) for tree in rf_model_real.estimators_])
    
    # Calculate the standard deviation of predictions as a measure of confidence
    confidence_std = np.std(all_tree_predictions, axis=0).mean()
    
    # Define a confidence score (this is a placeholder, modify as needed)
    confidence = max(0, 1 - confidence_std)  # Higher std deviation means lower confidence
    
    return predicted_x, predicted_y, predicted_z, confidence

@app.route('/process_phone_data', methods=['POST'])
def process_phone_data():
    try:
        data = request.get_json()
        
        # Extract sensor data from the request
        rssi_HUAWEI_CUTE_BD57 = data.get('rssi_HUAWEI_CUTE_BD57')
        rssi_YaSeR_Osama = data.get('rssi_YaSeR_Osama')
        rssi_MG2024 = data.get('rssi_MG2024')
        rssi_Samas_iPhone = data.get('rssi_Samas_iPhone')
        mag_x = data.get('mag_x')
        mag_y = data.get('mag_y')
        mag_z = data.get('mag_z')

        # Save the data to the temporaryData table
        new_temp_data = TemporaryData(
            rssi_HUAWEI_CUTE_BD57=rssi_HUAWEI_CUTE_BD57,
            rssi_YaSeR_Osama=rssi_YaSeR_Osama,
            rssi_MG2024=rssi_MG2024,
            rssi_Samas_iPhone=rssi_Samas_iPhone,
            mag_x=mag_x,
            mag_y=mag_y,
            mag_z=mag_z
        )
        db.session.add(new_temp_data)
        db.session.flush()
        db.session.commit()

        # Process the data with the ML model
        predicted_x, predicted_y, predicted_z, confidence = predict_location(data)

        # Update the PhoneData table if confidence is above the threshold
        if confidence >= 0.7:
            new_data = PhoneData(
                x=round(predicted_x),
                y=round(predicted_y),
                z=round(predicted_z),
                rssi_HUAWEI_CUTE_BD57=rssi_HUAWEI_CUTE_BD57,
                rssi_YaSeR_Osama=rssi_YaSeR_Osama,
                rssi_MG2024=rssi_MG2024,
                rssi_Samas_iPhone=rssi_Samas_iPhone,
                mag_x=mag_x,
                mag_y=mag_y,
                mag_z=mag_z
            )
            db.session.add(new_data)
            db.session.commit()

            # Update combined data
            new_combined_data = CombinedData(
                RSSI_TX_1=rssi_HUAWEI_CUTE_BD57,
                RSSI_TX_2=rssi_YaSeR_Osama,
                RSSI_TX_3=rssi_MG2024,
                RSSI_TX_4=rssi_Samas_iPhone
            )
            db.session.add(new_combined_data)
            db.session.commit()

        app.logger.info(f"Processed phone data: {data} with prediction ({predicted_x}, {predicted_y}, {predicted_z}) and confidence {confidence}")

        # Send the predicted location and confidence back to the phone
        return jsonify({'predicted_x': round(predicted_x), 'predicted_y': round(predicted_y), 'predicted_z': round(predicted_z), 'confidence': confidence}), 200

    except Exception as e:
        app.logger.error(f"Error processing phone data: {e}")
        return jsonify({'message': 'Error: Data processing failed'}), 500

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)

