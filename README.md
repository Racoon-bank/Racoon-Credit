# Racoon-Credit

graph TD

subgraph Service["Credit Service"]
api["api"]
tariff["tariff"]
rating["rating"]
application["application"]
schedule["schedule"]
masteraccount["masteraccount"]
integration["integration"]
security["security"]
persistence["persistence"]
end

CoreService["Core Service"]
InfoService["Info Service"]
PostgreSQL["PostgreSQL"]

api --> tariff
api --> rating
api --> application
api --> schedule
api --> masteraccount
api --> security

tariff --> persistence
rating --> persistence
application --> persistence
schedule --> persistence
masteraccount --> persistence

application --> integration
schedule --> integration
masteraccount --> integration

integration --> CoreService
security --> InfoService
persistence --> PostgreSQL
