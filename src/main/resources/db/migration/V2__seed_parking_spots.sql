INSERT INTO parking_spots (code, vehicle_type, occupancy_status, in_maintenance)
SELECT 'A' || LPAD(number::TEXT, 2, '0'), 'MOTORBIKE', 'AVAILABLE', FALSE
FROM GENERATE_SERIES(1, 20) AS number
ON CONFLICT (code) DO NOTHING;

INSERT INTO parking_spots (code, vehicle_type, occupancy_status, in_maintenance)
SELECT 'B' || LPAD(number::TEXT, 2, '0'), 'CAR', 'AVAILABLE', FALSE
FROM GENERATE_SERIES(1, 10) AS number
ON CONFLICT (code) DO NOTHING;
