from flask_sqlalchemy import SQLAlchemy

db = SQLAlchemy()

class SimulatedData(db.Model):
    __tablename__ = 'SimulatedData'
    id = db.Column(db.Integer, primary_key=True)
    meanToA_TX_1 = db.Column(db.Float)
    stdToA_TX_1 = db.Column(db.Float)
    maxAmplitude_TX_1 = db.Column(db.Float)
    meanAmplitude_TX_1 = db.Column(db.Float)
    stdAmplitude_TX_1 = db.Column(db.Float)
    meanToA_TX_2 = db.Column(db.Float)
    stdToA_TX_2 = db.Column(db.Float)
    maxAmplitude_TX_2 = db.Column(db.Float)
    meanAmplitude_TX_2 = db.Column(db.Float)
    stdAmplitude_TX_2 = db.Column(db.Float)
    meanToA_TX_3 = db.Column(db.Float)
    stdToA_TX_3 = db.Column(db.Float)
    maxAmplitude_TX_3 = db.Column(db.Float)
    meanAmplitude_TX_3 = db.Column(db.Float)
    stdAmplitude_TX_3 = db.Column(db.Float)
    meanToA_TX_4 = db.Column(db.Float)
    stdToA_TX_4 = db.Column(db.Float)
    maxAmplitude_TX_4 = db.Column(db.Float)
    meanAmplitude_TX_4 = db.Column(db.Float)
    stdAmplitude_TX_4 = db.Column(db.Float)
    RSSI_TX_1 = db.Column(db.Float)
    RSSI_TX_2 = db.Column(db.Float)
    RSSI_TX_3 = db.Column(db.Float)
    RSSI_TX_4 = db.Column(db.Float)
    x = db.Column(db.Float)
    y = db.Column(db.Float)
    z = db.Column(db.Float)

class PhoneData(db.Model):
    __tablename__ = 'PhoneData'
    id = db.Column(db.Integer, primary_key=True)
    x = db.Column(db.Float)
    y = db.Column(db.Float)
    z = db.Column(db.Float)
    rssi_HUAWEI_CUTE_BD57 = db.Column(db.Float)
    rssi_YaSeR_Osama = db.Column(db.Float)
    rssi_MG2024 = db.Column(db.Float)
    rssi_Samas_iPhone = db.Column(db.Float)
    mag_x = db.Column(db.Float)
    mag_y = db.Column(db.Float)
    mag_z = db.Column(db.Float)

class CombinedData(db.Model):
    __tablename__ = 'CombinedData'
    id = db.Column(db.Integer, primary_key=True)
    RSSI_TX_1 = db.Column(db.Float)
    RSSI_TX_2 = db.Column(db.Float)
    RSSI_TX_3 = db.Column(db.Float)
    RSSI_TX_4 = db.Column(db.Float)

class TemporaryData(db.Model):
    __tablename__ = 'temporaryData'
    id = db.Column(db.Integer, primary_key=True, autoincrement=True)
    rssi_HUAWEI_CUTE_BD57 = db.Column(db.Float, nullable=True)
    rssi_YaSeR_Osama = db.Column(db.Float, nullable=True)
    rssi_MG2024 = db.Column(db.Float, nullable=True)
    rssi_Samas_iPhone = db.Column(db.Float, nullable=True)
    mag_x = db.Column(db.Float, nullable=False)
    mag_y = db.Column(db.Float, nullable=False)
    mag_z = db.Column(db.Float, nullable=False)
    timestamp = db.Column(db.DateTime, default=db.func.current_timestamp(), nullable=False)
