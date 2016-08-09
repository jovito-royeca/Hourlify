/*
 * Generates sample data and uploads to MySQL database
 */
package com.hourlify.dataloader;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

/**
 *
 * @author Jovito Royeca
 */
public class DataLoader {
    public static final String Host = "127.0.0.1:3306";
    public static final String Database = "hourlifydb";
    public static final String DatabaseUser = "hourlify";
    public static final String DatabasePassword = "password";
    public static final String JDBC_Driver = "org.mariadb.jdbc.Driver";
    public static final String JDBC_URL = "jdbc:mariadb://"+Host+"/"+Database;
    
    private Connection conn;
    
    public static void main(String[] args) throws Exception {
        DataLoader loader = new DataLoader();
                
        loader.setupDatabase();
//        loader.createEmployees();
//        loader.createEmployeeSkills();
//        loader.createServiceOrders();
//        loader.createServiceOrderTasks();
//        loader.createServiceOrderSkills();
        for (int i=2012; i<2017; i++) {
            loader.createTimesheets(i);
        }
        loader.cleanupDatabase();
    }
    
    /* Database methods */
    public void setupDatabase() throws Exception {
        Class.forName(JDBC_Driver);
        conn = DriverManager.getConnection(JDBC_URL, DatabaseUser, DatabasePassword);
    }
    
    public void cleanupDatabase() throws Exception {
        conn.close();
    }
    
    /* Data modification methods */
    
    public void createEmployees() throws Exception {
        int employeeCount = 200;
        List lastNames = arrayFromFile("lastname.csv");
        List firstNames = arrayFromFile("firstname.csv");
        
        PreparedStatement ps = conn.prepareStatement("INSERT INTO employee(employeeID, lastName, firstName) VALUES(?,?,?)");
        for (int i=0; i<employeeCount; i++) {
            Random rand1 = new Random();
            Random rand2 = new Random();

            ps.setInt(1, i+1000);
            ps.setString(2, (String) lastNames.get(rand1.nextInt(lastNames.size()-1)));
            ps.setString(3, (String) firstNames.get(rand2.nextInt(firstNames.size()-1)));
            ps.execute();
        }
        ps.close();
    }
    
    public void createEmployeeSkills() throws Exception { 
        int employeeCount = 200;
        String[] skillTypes = {"Application Development",
                               "Application Support",
                               "Infrastructure Support",
                               "Management",
                               "Project Management",
                               "Sales and Marketing",
                               "SAP",
                               "Service Support"};
        
        String selectEmployees = "select employeeID from employee where employeeID not in (select employeeID from employeeSkill)";
        PreparedStatement psInsertEmployeeSkill = conn.prepareStatement("insert into employeeSkill(employeeID, skillID, skillLevel) values(?,?,?)");
        List<List> skills = getRows("skill", null);
        
        for (List skill : skills) {
            int skillID = (Integer)skill.get(0);
            double workforcePercentage = (Double)skill.get(3);
            String skillType = (String)skill.get(4);
            
            PreparedStatement psSelectEmployees = conn.prepareStatement(selectEmployees + " limit " + (int)(employeeCount * workforcePercentage));
            ResultSet rsSelectEmployees = psSelectEmployees.executeQuery();
            while (rsSelectEmployees.next()) {
                psInsertEmployeeSkill.setInt(1, rsSelectEmployees.getInt(1));
                psInsertEmployeeSkill.setInt(2, skillID);
                psInsertEmployeeSkill.setInt(3, 10);
                psInsertEmployeeSkill.execute();
            }
            rsSelectEmployees.close();
        }
        psInsertEmployeeSkill.close();
    }
    
    public void createServiceOrders() throws Exception { 
        PreparedStatement psInsertServiceOrder = conn.prepareStatement("insert into serviceOrder(clientID, serviceOrderName) values(?,?)");
        List<List> clients = getRows("client", null);
        
        for (List client: clients) {
            int clientID = (Integer)client.get(0);
            String clientName = (String)client.get(1);
            String[] projects = {"Project 1", "Project 2", "Project 3"};
            
            if (clientName.equals("KAISA")) {
                psInsertServiceOrder.setInt(1, clientID);
                psInsertServiceOrder.setString(2, "KCCI");
                psInsertServiceOrder.execute();
            } else {
                for (String p : projects) {
                    psInsertServiceOrder.setInt(1, clientID);
                    psInsertServiceOrder.setString(2, new String(clientID + "_" + p));
                    psInsertServiceOrder.execute();
                }
            }
        }
        psInsertServiceOrder.close();
    }
    
    public void createServiceOrderTasks() throws Exception { 
        PreparedStatement psInsertServiceOrderTask = conn.prepareStatement("insert into serviceOrderTask(serviceOrderID, taskID) values(?,?)");
        List<List> serviceOrders = getRows("serviceOrder", null);
        List<List> tasks = getRows("task", null);
        
        for (List serviceOrder : serviceOrders) {
            int serviceOrderID = (Integer)serviceOrder.get(0);
            int clientID = (Integer)serviceOrder.get(1);

            for (List task : tasks) {
                int taskID = (Integer)task.get(0);
                String taskCode = (String)task.get(1);
            
                if (clientID == 1 /*Kaisa*/) {
                    if (taskCode.equals("PRO") ||
                        taskCode.equals("SUP")) {
                        continue;
                    }
                    psInsertServiceOrderTask.setInt(1, serviceOrderID);
                    psInsertServiceOrderTask.setInt(2, taskID);
                    psInsertServiceOrderTask.execute();
                        
                } else {
                    if (taskCode.equals("PRO") ||
                        taskCode.equals("RSC") ||
                        taskCode.equals("SUP")) {
                        psInsertServiceOrderTask.setInt(1, serviceOrderID);
                        psInsertServiceOrderTask.setInt(2, taskID);
                        psInsertServiceOrderTask.execute();
                    }
                }
            }
        }
        
        psInsertServiceOrderTask.close();
    }
    
    public void createServiceOrderSkills() throws Exception { 
        PreparedStatement psInsertServiceOrderSkill = conn.prepareStatement("insert into serviceOrderSkill(serviceOrderID, skillID) values(?,?)");
        List<List> serviceOrders = getRows("serviceOrder", null);
        List<List> skills = getRows("skill", null);
        
        for (List serviceOrder : serviceOrders) {
            Integer serviceOrderID = (Integer)serviceOrder.get(0);
            String serviceOrderName = (String)serviceOrder.get(2);
            
            if (!serviceOrderName.equals("KCCI")) {
                List<Integer> skillIDs = new ArrayList<>();
                
                // get the skillIDs
                List<Integer> appDevIDs = new ArrayList<>();
                List<Integer> appSupportIDs = new ArrayList<>();
                List<Integer> pmIDs = new ArrayList<>();
                List<Integer> sapIDs = new ArrayList<>();
                for (List skill : skills) {
                    Integer skillID = (Integer)skill.get(0);
                    String skillType = (String)skill.get(4);
                    
                    if (skillType.equals("Application Development")) {
                        appDevIDs.add(skillID);
                    } else if (skillType.equals("Application Support")) {
                        appSupportIDs.add(skillID);
                    } else if (skillType.equals("Project Management")) {
                        pmIDs.add(skillID);
                    } else if (skillType.equals("SAP")) {
                        sapIDs.add(skillID);
                    }
                }
                Random random = new Random();
                int  n = random.nextInt(appDevIDs.size()) + 1;
                for (int i=0; i<n; i++) {
                    int m = random.nextInt(n) + 1;
                    
                    if (!skillIDs.contains(appDevIDs.get(m-1))) {
                        skillIDs.add(appDevIDs.get(m-1));
                    }
                }
                random = new Random();
                n = random.nextInt(appSupportIDs.size()) + 1;
                for (int i=0; i<n; i++) {
                    int m = random.nextInt(n) + 1;
                    
                    if (!skillIDs.contains(appSupportIDs.get(m-1))) {
                        skillIDs.add(appSupportIDs.get(m-1));
                    }
                }
                skillIDs.addAll(pmIDs);
                random = new Random();
                n = random.nextInt(sapIDs.size()) + 1;
                for (int i=0; i<n; i++) {
                    int m = random.nextInt(n) + 1;
                    
                    if (!skillIDs.contains(sapIDs.get(m-1))) {
                        skillIDs.add(sapIDs.get(m-1));
                    }
                }
                
                for (Integer skillID : skillIDs) {
                    psInsertServiceOrderSkill.setInt(1, serviceOrderID);
                    psInsertServiceOrderSkill.setInt(2, skillID);
                    psInsertServiceOrderSkill.execute();
                }
            }
        }
        
        psInsertServiceOrderSkill.close();
    }
    
    public void createTimesheets(int year) throws Exception {
        List<List> employees = getRows("employee", null);
        List<List> serviceOrders = getRows("serviceOrder", null);
        List<List> serviceOrderSkills = getRows("serviceOrderSkill", null);
        List<List> tasks = getRows("task", null);
        
        // KCCI serviceOrder
        int serviceOrderID = -1;
        for (List serviceOrder : serviceOrders) {
            String serviceOrderName = (String)serviceOrder.get(2);
            if (serviceOrderName.equals("KCCI")) {
                serviceOrderID = (Integer)serviceOrder.get(0);
                break;
            }
        }
                    
        // holiday task 
        int taskID = -1;
        for (List task : tasks) {
            String taskCode = (String)task.get(1);
            if (taskCode.equals("HOL")) {
                taskID = (Integer)task.get(0);
                break;
            }
        }
                    
        // loop through the months in the year
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        for (int i=0; i<12; i++) {
            
            cal.set(Calendar.MONTH, i);
            for (int j=1; j<=cal.getMaximum(Calendar.DAY_OF_MONTH); j++) {
                cal.set(Calendar.DAY_OF_MONTH, j);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                
                java.util.Date date = cal.getTime();
                String holiday = isHoliday(date);
                
                if (holiday != null) {
                    // create holidays for each employee
                    for (List employee : employees) {
                        System.out.println("Holiday: " + date + " - " + employee.get(1) + ", " + employee.get(2));
                        insertTimesheet(
                                new Date(date.getTime()),
                                (Integer)employee.get(0),
                                serviceOrderID,
                                taskID,
                                holiday,
                                0,
                                0
                        );
                    }
                    
                } else {
                    
                }
            }
        }
    }
    
    public void insertTimesheet(
            Date date,
            Integer employeeID,
            Integer serviceOrderID,
            Integer taskID,
            String description,
            int hours,
            int minutes) throws Exception {
        PreparedStatement psInsertTimesheet = conn.prepareStatement("insert into timesheet(date, employeeID, serviceOrderID, taskID, description, hours, minutes) values(?,?,?,?,?,?,?)");
        
        psInsertTimesheet.setDate(1, date);
        psInsertTimesheet.setInt(2, employeeID);
        psInsertTimesheet.setInt(3, serviceOrderID);
        psInsertTimesheet.setInt(4, taskID);
        psInsertTimesheet.setString(5, description);
        psInsertTimesheet.setInt(6, hours);
        psInsertTimesheet.setInt(7, minutes);
        psInsertTimesheet.execute();
                        
        psInsertTimesheet.close();
    }
    
    /* Utility methods */
    
    public List<List> getRows(String table, String whereClause) throws Exception {
        List<List> list = new ArrayList();
        String sql = "select * from " + table;
        if (whereClause != null) {
            sql += (" where " + whereClause);
        }
        PreparedStatement psSelect = conn.prepareStatement(sql);
        ResultSet rsSelect = psSelect.executeQuery();
        ResultSetMetaData metaData = rsSelect.getMetaData();
        
        while (rsSelect.next()) {
            List<Object> rows = new ArrayList();
            
            for (int i=0; i<metaData.getColumnCount(); i++) {
                switch (metaData.getColumnType(i+1)) {
                    case Types.INTEGER:
                    case Types.BIGINT: {
                        rows.add(rsSelect.getInt(i+1));
                        break;
                    }
                    case Types.VARCHAR: {
                        rows.add(rsSelect.getString(i+1));
                        break;
                    }
                    case Types.DATE: {
                        rows.add(rsSelect.getDate(i+1));
                        break;
                    }
                    case Types.TIMESTAMP: {
                        rows.add(rsSelect.getTimestamp(i+1));
                        break;
                    }
                    case Types.TIME: {
                        rows.add(rsSelect.getTime(i+1));
                        break;
                    }
                    case Types.DOUBLE: {
                        rows.add(rsSelect.getDouble(i+1));
                        break;
                    }
                    case Types.FLOAT: {
                        rows.add(rsSelect.getFloat(i+1));
                        break;
                    }
                    default: {
                        rows.add(rsSelect.getObject(i+1));
                        break;
                    }
                }
            }
            
            list.add(rows);
        }
        
        rsSelect.close();
        psSelect.close();
        
        return list;
    }
    
    public String isHoliday(java.util.Date date) throws Exception {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH)+1;
        int day = cal.get(Calendar.DAY_OF_MONTH);
        String whereClause = "holidate = '" + (year+"-"+month+"-"+day) + "'";
        
        List<List> holidays = getRows("holiday", whereClause);
        
        for (List holiday : holidays) {
            java.util.Date holidate = new java.util.Date(((java.sql.Date)holiday.get(2)).getTime());
            if (holidate.compareTo(date) == 0) {
                return (String) holiday.get(1);
            }
        }
        
        return null;
    }
    
    public static List<String> arrayFromFile(String fileName) throws Exception {
        ArrayList<String> array = new ArrayList();
        InputStream in = DataLoader.class.getClassLoader().getResourceAsStream(fileName);
        BufferedReader br = new BufferedReader(new InputStreamReader(in));

        String line;
        while ((line = br.readLine()) != null) {
            array.add(line);
        }
        
        return array;
    }
}
