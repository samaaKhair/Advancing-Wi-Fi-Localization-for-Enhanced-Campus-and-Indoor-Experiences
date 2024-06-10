import pandas as pd
import sqlite3
from sklearn.preprocessing import MinMaxScaler, StandardScaler
from sklearn.model_selection import train_test_split
from sklearn.ensemble import RandomForestRegressor
from sklearn.metrics import mean_squared_error
import numpy as np
import joblib

# Connect to SQLite database
conn = sqlite3.connect('D:\ZC\Y5\Spring\Graduation Project\Central Unit\instance\localization.db')

# Read data from the database
simulation_features = pd.read_sql_query("SELECT * FROM SimulatedData", conn)
combined_features = pd.read_sql_query("SELECT * FROM CombinedData", conn)
phone_data = pd.read_sql_query("SELECT * FROM PhoneData", conn)

# Extract simulation labels
simulation_labels = simulation_features[['x', 'y', 'z']]
simulation_features = simulation_features.drop(columns=['x', 'y', 'z'])

def mapping_Ycoordinates(y):
    m = -13 / 7534
    b = -m * 15400
    return m * y + b

def mapping_Xcoordinates(x):
    m = -17 / 9700
    b = -m * 13900
    return m * x + b

def mapping_Zcoordinates(z):
    z = 1.5
    return z

# Data Preprocessing
phone_data_nullFree = phone_data.dropna()

first_three_columns = phone_data_nullFree.iloc[:, 1:4]
rest_of_columns = phone_data_nullFree.iloc[:, 4:]

# Group by the first three columns and average the rest
phone_data_cleaned = phone_data_nullFree.groupby(list(first_three_columns.columns)).mean().reset_index()

realWorld_features = phone_data_cleaned.iloc[:, 4:]
realWorld_labels = phone_data_cleaned.iloc[:, 0:3]

X_train_real, X_test_real, Y_train_real, Y_test_real = train_test_split(realWorld_features, realWorld_labels, test_size=0.35, random_state=42)

# Define and train Random Forest model
rf_model_real = RandomForestRegressor(n_estimators=100, random_state=42, oob_score=True)
rf_model_real.fit(X_train_real, Y_train_real)

# Save the model
joblib.dump(rf_model_real, 'rf_model_real.pkl')

# Predict and evaluate
y_pred_real = rf_model_real.predict(X_test_real)
mse_real = mean_squared_error(Y_test_real, y_pred_real)
print(f'Random Forest Test MSE: {mse_real}')

# Mapping Coordinates
simulation_labels["x"] = simulation_labels["x"].apply(lambda x: round(mapping_Xcoordinates(x)))
simulation_labels["y"] = simulation_labels["y"].apply(lambda y: round(mapping_Ycoordinates(y)))
simulation_labels["z"] = simulation_labels["z"].apply(mapping_Zcoordinates)

X_train_sim, X_test_sim, Y_train_sim, Y_test_sim = train_test_split(simulation_features, simulation_labels, test_size=0.35, random_state=42)

# Define and train Random Forest model
rf_model = RandomForestRegressor(n_estimators=100, random_state=42)
rf_model.fit(X_train_sim, Y_train_sim)

# Predict and evaluate
Y_pred_sim = rf_model.predict(X_test_sim)
Y_pred_sim = np.round(Y_pred_sim).astype(int)
mse_sim = mean_squared_error(Y_test_sim, Y_pred_sim)
print(f'Random Forest Test MSE: {mse_sim}')

# Combined Datasets
sim_rssi_features = simulation_features.iloc[:, -4:]
real_rssi_features = realWorld_features.iloc[:, :4]

combined_labels = np.concatenate((simulation_labels, realWorld_labels))
combined_features = np.vstack((sim_rssi_features, real_rssi_features))

# Initialize the MinMaxScaler
scaler = MinMaxScaler()

# Fit the scaler on the combined data
scaler.fit(combined_features)

# Transform both datasets
sim_rssi_features_normalized = scaler.transform(sim_rssi_features)
real_rssi_features_normalized = scaler.transform(real_rssi_features)

combined_features_normalized = np.concatenate((sim_rssi_features_normalized, real_rssi_features_normalized))

X_train_combined, X_test_combined, Y_train_combined, Y_test_combined = train_test_split(combined_features_normalized, combined_labels, test_size=0.35, random_state=42)

# Define and train Random Forest model
rf_model = RandomForestRegressor(n_estimators=100, random_state=42)
rf_model.fit(X_train_combined, Y_train_combined)

# Predict and evaluate
Y_pred_combined = rf_model.predict(X_test_combined)
mse_combined = mean_squared_error(Y_test_combined, Y_pred_combined)
print(f'Random Forest Test MSE: {mse_combined}')

# Close the database connection
conn.close()
