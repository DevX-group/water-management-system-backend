CREATE TABLE monthly_reports (
    id SERIAL PRIMARY KEY,
    year INT NOT NULL,
    month VARCHAR(10) NOT NULL,
    usage INT NOT NULL,
    revenue INT NOT NULL
);

select *
from monthly_reports

GRANT ALL PRIVILEGES ON TABLE monthly_reports TO hydropay_user;


--monthly report query to replace later

SELECT
  TO_CHAR(record_date, 'Mon') AS month,
  SUM(usage) AS usage,
  SUM(amount) AS revenue
FROM usage_records
WHERE EXTRACT(YEAR FROM record_date) = 2026
GROUP BY TO_CHAR(record_date, 'Mon'), EXTRACT(MONTH FROM record_date)
ORDER BY EXTRACT(MONTH FROM record_date);