-- Motivo que el superadmin/admin escribe al dar de baja un operador o un taxi, para que la
-- persona afectada pueda verlo (ver AuthService.login y DriverPortalController.myTaxi). Se borra
-- solo al reactivar (ver DriverService/TaxiService.setActive).
ALTER TABLE driver_profiles ADD COLUMN deactivation_reason VARCHAR(300);
ALTER TABLE taxis ADD COLUMN deactivation_reason VARCHAR(300);
