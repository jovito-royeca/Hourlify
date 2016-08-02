/*
 * Generates sample data and uploads to MySQL database
 */
package com.jovitoroyeca.datasource;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.ArrayList;
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
        loader.createTimesheets(2016);
        loader.cleanupDatabase();
    }
    
    public void setupDatabase() throws Exception {
        Class.forName(JDBC_Driver);
        conn = DriverManager.getConnection(JDBC_URL, DatabaseUser, DatabasePassword);
    }
    
    public void cleanupDatabase() throws Exception {
        conn.close();
    }
    
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
        PreparedStatement psSelectSkills = conn.prepareStatement("select * from skill");
        PreparedStatement psInsertEmployeeSkill = conn.prepareStatement("insert into employeeSkill(employeeID, skillID, skillLevel) values(?,?,?)");
        ResultSet rsSelectSkills = psSelectSkills.executeQuery();
        
        while (rsSelectSkills.next()) {
            int skillID = rsSelectSkills.getInt(1);
            double workforcePercentage = rsSelectSkills.getDouble(4);
            String skillType = rsSelectSkills.getString(5);
            
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
        rsSelectSkills.close();
    }
    
    public void createServiceOrders() throws Exception { 
        PreparedStatement psSelectClients = conn.prepareStatement("select * from client");
        PreparedStatement psInsertServiceOrder = conn.prepareStatement("insert into serviceOrder(clientID, serviceOrderName) values(?,?)");
        ResultSet rsSelectClients = psSelectClients.executeQuery();
        
        while (rsSelectClients.next()) {
            int clientID = rsSelectClients.getInt(1);
            String clientName = rsSelectClients.getString(2);
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
        rsSelectClients.close();
    }
    
    public void createServiceOrderTasks() throws Exception { 
        PreparedStatement psSelectServiceOrders = conn.prepareStatement("select * from serviceOrder");
        PreparedStatement psSelectTasks = conn.prepareStatement("select * from task");
        PreparedStatement psInsertServiceOrderTask = conn.prepareStatement("insert into serviceOrderTask(serviceOrderID, taskID) values(?,?)");
        ResultSet rsSelectServiceOrders = psSelectServiceOrders.executeQuery();
        
        while (rsSelectServiceOrders.next()) {
            int serviceOrderID = rsSelectServiceOrders.getInt(1);
            int clientID = rsSelectServiceOrders.getInt(2);

            ResultSet rsSelectTasks = psSelectTasks.executeQuery();
            while (rsSelectTasks.next()) {
                int taskID = rsSelectTasks.getInt(1);
                String taskCode = rsSelectTasks.getString(2);
            
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
            rsSelectTasks.close();
        }
        
        psInsertServiceOrderTask.close();
        rsSelectServiceOrders.close();
        psSelectServiceOrders.close();
        psSelectTasks.close();
    }
    
    public void createTimesheets(int year) throws Exception {
        PreparedStatement psSelectHolidays = conn.prepareStatement("select holidate from holiday where holidate between ? AND ?");
        psSelectHolidays.setString(1, year+"-01-01");
        psSelectHolidays.setString(2, year+"-12-31");
        ResultSet rsSelectHolidays = psSelectHolidays.executeQuery();
        
        while (rsSelectHolidays.next()) {
            System.out.println(rsSelectHolidays.getDate(1));
        }
    }
    
    public static List<String> arrayFromFile(String fileName) throws Exception {
        ArrayList<String> array = new ArrayList();
        InputStream in = SheetsQuickstart.class.getClassLoader().getResourceAsStream(fileName);
        BufferedReader br = new BufferedReader(new InputStreamReader(in));

        String line;
        while ((line = br.readLine()) != null) {
            array.add(line);
        }
        
        return array;
    }
}
