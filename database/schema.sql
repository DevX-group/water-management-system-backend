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
