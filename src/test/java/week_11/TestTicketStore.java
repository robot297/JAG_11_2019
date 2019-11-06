package week_11;

import org.junit.Before;
import org.junit.Test;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static week_11.TestConfig.timeout;

import static org.junit.Assert.*;
import static week_11.TicketUtil.sameOpenTicket;

public class TestTicketStore {
    
    @Before()
    public void clearTicketStore() throws Exception {
        TicketUtil.clearStore();
    }
    
    @Test(timeout = timeout)
    public void testColumnOrder() throws Exception {
        
        Ticket t = new Ticket("Problem", 4, "Me", new Date(1500000));
        t.setStatus(Ticket.TicketStatus.RESOLVED);
        t.setDateResolved(new Date(2000000));
        t.setResolution("Fixed");
        
        TicketStore store = new TicketStore(TestConfig.TEST_DB_URI);
        store.add(t);
        
        Ticket fromStore = store.getTicketById(t.getTicketID());
        assertTrue(sameOpenTicket(t, fromStore));
    }
    
    
    @Test(timeout = timeout, expected = SQLException.class)
    public void testStatus() throws Exception {
        
        try (Statement statement = DriverManager.getConnection(TestConfig.TEST_DB_URI).createStatement())  {
            statement.executeUpdate("INSERT INTO tickets values ('Problem', 4, 'me', 500000, 'fixed', 600000, 'PIZZA' )");
        } catch (SQLException e ) {
            System.out.println("status," + e);
            throw e;
        }
    }
    
    @Test(timeout = timeout)
    public void testInsertToTable() throws Exception {
        
        int newid = 0;
        try (Statement statement = DriverManager.getConnection(TestConfig.TEST_DB_URI).createStatement())  {
            statement.executeUpdate("INSERT INTO tickets values ('Problem', 4, 'Me', 500000, 'Fixed', 600000, 'RESOLVED' )");
            ResultSet rs = statement.getGeneratedKeys();
            if (rs.next()) {
                newid = rs.getInt(1);
            } else {
                throw new SQLException("no row id generated in table");
            }
        } catch (SQLException e ) {
            throw e;
        }
        
        TicketStore store = new TicketStore(TestConfig.TEST_DB_URI);
        Ticket ticket = store.getTicketById(newid);
        
        assertEquals("Problem", ticket.getDescription());
        assertEquals(4, ticket.getPriority());
        assertEquals("Me", ticket.getReporter());
        assertEquals("Fixed", ticket.getResolution());
        assertEquals(newid, ticket.getTicketID());
        assertEquals(Ticket.TicketStatus.RESOLVED, ticket.getStatus());
        assertEquals(new Date(500000), ticket.getDateReported());
        assertEquals(new Date(600000), ticket.getDateResolved());
        
    }
    
    
    @Test(timeout=timeout, expected = SQLException.class)
    public void testNoReporter() throws Exception {
        TicketStore store = new TicketStore(TestConfig.TEST_DB_URI);
        Ticket t1 = new Ticket("Invalid Reporter", 3, null, new Date());
        store.add(t1);
    }
    
    
    @Test(timeout=timeout, expected = SQLException.class)
    public void testNoDescription() throws Exception {
        TicketStore store = new TicketStore(TestConfig.TEST_DB_URI);
        Ticket t1 = new Ticket(null, 3, "Me", new Date());
        store.add(t1);
    }
    
    
    @Test(timeout=timeout, expected = SQLException.class)
    public void testPriorityRangeZero() throws Exception {
        TicketStore store = new TicketStore(TestConfig.TEST_DB_URI);
        Ticket t1 = new Ticket("Invalid priority", 0, "me", new Date());
        store.add(t1);
    }
    
    @Test(timeout=timeout, expected = SQLException.class)
    public void testPriorityRangeMinus1() throws Exception {
        TicketStore store = new TicketStore(TestConfig.TEST_DB_URI);
        Ticket t1 = new Ticket("Invalid priority", -1, "me", new Date());
        store.add(t1);
    }
    
    @Test(timeout=timeout, expected = SQLException.class)
    public void testPriorityRangeSix() throws Exception {
        TicketStore store = new TicketStore(TestConfig.TEST_DB_URI);
        Ticket t1 = new Ticket("Invalid priority", 6, "me", new Date());
        store.add(t1);
    }
    
    @Test(timeout=timeout, expected = SQLException.class)
    public void testPriorityRangeSixty() throws Exception {
        TicketStore store = new TicketStore(TestConfig.TEST_DB_URI);
        Ticket t1 = new Ticket("Invalid priority", 60, "me", new Date());
        store.add(t1);
    }
    
    
    @Test(timeout=timeout)
    public void testSearchDescriptionEmptyStore() throws Exception {
        TicketStore store = new TicketStore(TestConfig.TEST_DB_URI);
        
        // Any searches on an empty list should return an empty list.
        assertEquals(0, store.searchByDescription("office").size());
        assertEquals(0, store.searchByDescription("").size());
        assertEquals(0, store.searchByDescription(null).size());
        
    }
    
    
    @Test(timeout=timeout)
    public void testSearchDescriptionExpectedFound() throws Exception {
        
        TicketStore store = new TicketStore(TestConfig.TEST_DB_URI);
        
        Ticket test1 = new Ticket("The server is on fire", 1, "1", new Date());
        Ticket test2 = new Ticket("Server keeps rebooting", 2, "2", new Date());
        Ticket test3 = new Ticket("Mouse mat stolen FROM MY SERVER", 3, "3", new Date());
        Ticket test4 = new Ticket("Critical security updates", 1, "3", new Date());
        
        //Add these tickets
        store.add(test1); store.add(test2); store.add(test3); store.add(test4);
        
        System.out.println("This test uses the following example tickets\n" + "\n" + test1 + "\n" + test2 + "\n" + test3 + "\n" + test4);
        
        // Search for 'server'. Should not be case sensitive, return test1 and test2 and test3
        
        List<Ticket> results = store.searchByDescription("Server");
        String msg = "Return a List of the 3 Tickets whose description contains the search text 'Server'. " +
                "If no matches, return an empty list. Your search should not be case sensitive";
        assertNotNull(msg, results);
        assertEquals(msg, results.size(), 3);   // 3 results
        
        List<Integer> ids = results.stream().map(Ticket::getTicketID).collect(Collectors.toList());
        assertTrue(msg, ids.contains(test1.getTicketID()));
        assertTrue(msg, ids.contains(test2.getTicketID()));
        assertTrue(msg, ids.contains(test3.getTicketID()));
        assertFalse(msg, ids.contains(test4.getTicketID()));
        
        results = store.searchByDescription("SeCuRiTy UpDaTeS");
        
        msg = "Return a LinkedList of 1 Ticket whose description contains the search text 'security updates'. Your search should not be case sensitive";
        assertNotNull(msg, results);
        assertEquals(msg, results.size(), 1);   // just one ticket
        assertEquals(msg, test4.getTicketID(), results.get(0).getTicketID());
        
        
        results = store.searchByDescription("aT stOl");   // matches "Mouse mat stolen"
        
        msg = "Return a LinkedList of 1 Ticket whose description contains the search text 'aT stOl'. \n" +
                "This will match a ticket with description \"Mouse mat stolen\". Your search should not be case sensitive";
        assertNotNull(msg, results);
        assertEquals(msg, results.size(), 1);   // just one ticket
        assertEquals(msg, test3.getTicketID(), results.get(0).getTicketID());
        
    }
    
    
    @Test(timeout=timeout)
    public void testSearchDescriptionNotFound() throws Exception {
        
        TicketStore store = new TicketStore(TestConfig.TEST_DB_URI);
        
        Ticket test1 = new Ticket("The server is on fire", 1, "1", new Date());
        Ticket test2 = new Ticket("Server keeps rebooting", 2, "2", new Date());
        Ticket test3 = new Ticket("Mouse mat stolen", 3, "3", new Date());
        Ticket test4 = new Ticket("Critical security updates", 1, "3", new Date());
        
        //Add these tickets
        
        store.add(test1); store.add(test2); store.add(test3); store.add(test4);
        
        System.out.println("This test uses the following example tickets\n" + "\n" + test1 + "\n" + test2 + "\n" + test3 + "\n" + test4);
        
        // Search for something not in the list
        List<Ticket> results = store.searchByDescription("Powerpoint");
        String msg = "Search for 'Powerpoint' should return a List of results. If no matches, return an empty list.";
        assertNotNull(msg, results);
        assertEquals(msg, results.size(), 0);   // No results
        
        // Empty string - should return empty list
        results = store.searchByDescription("");
        assertNotNull("A search for an empty string should return an empty list", results);
        assertEquals("A search for an empty string should return an empty list", results.size(), 0);   // No results
        
        
        // Null string - should return empty list
        results = store.searchByDescription(null);
        assertNotNull("A search for null string should return an empty list", results);
        assertEquals("A search for null string should return an empty list", results.size(), 0);   // No results
        
    }
    
    
    @Test(timeout=timeout)
    public void testResolveTicketThatExists() throws Exception {
        TicketStore store = new TicketStore(TestConfig.TEST_DB_URI);
        
        Ticket testPr1 = new Ticket("The server is on fire", 1, "A. Reporter", new Date());
        Ticket testPr5 = new Ticket("Mouse mat stolen", 5, "B. Reporter", new Date());
        Ticket testPr3 = new Ticket("Word needs updating", 3, "C. Reporter", new Date());
        
        store.add(testPr1); store.add(testPr5); store.add(testPr3);
        
        testPr3.setDateResolved(new Date());
        testPr3.setResolution("Word has been updated");
        testPr3.setStatus(Ticket.TicketStatus.RESOLVED);
        
        boolean updated = store.updateTicket(testPr3);
        
        Ticket savedTicket = store.getTicketById(testPr3.getTicketID());
        
        assertTrue(sameOpenTicket(savedTicket, testPr3, 500));
        
        assertTrue(updated);
        
    }
    
    @Test(timeout=timeout)
    public void testResolveTicketThatDoesNotExist() throws Exception {
        TicketStore store = new TicketStore(TestConfig.TEST_DB_URI);
        
        Ticket testPr1 = new Ticket("The server is on fire", 1, "A. Reporter", new Date());
        store.add(testPr1);
        
        Ticket notSaved = new Ticket("Not saved", 1, "Not saved", new Date());
        
        boolean updated = store.updateTicket(notSaved);
        assertFalse(updated);
        
    }
    
    
    @Test
    public void testTicketStoreTicketsSortedPriorityOrder() throws Exception  {
        
        TicketStore store = new TicketStore(TestConfig.TEST_DB_URI);
        
        // Test tickets with all different priorities
        Ticket testPr1 = new Ticket("The server is on fire", 1, "A. Reporter", new Date());
        Ticket testPr5 = new Ticket("Mouse mat stolen", 5, "B. Reporter", new Date());
        Ticket testPr3 = new Ticket("Word needs updating", 3, "C. Reporter", new Date());
        
        //Add these tickets. Assert they are added with lowest priority first
        store.add(testPr1); store.add(testPr5); store.add(testPr3);
        
        List<Ticket> allTickets = store.getAllOpenTickets();
        
        assertTrue(sameOpenTicket(allTickets.get(0), testPr1, true));
        assertTrue(sameOpenTicket(allTickets.get(1), testPr3, true));
        assertTrue(sameOpenTicket(allTickets.get(2), testPr5, true));
        
    }
    
    
    @Test
    public void testTicketStoreGetTicketByID() throws Exception {
        
        TicketStore store = new TicketStore(TestConfig.TEST_DB_URI);
        
        Ticket testPr1 = new Ticket("The server is on fire", 1, "A. Reporter", new Date());
        Ticket testPr5 = new Ticket("Mouse mat stolen", 5, "B. Reporter", new Date());
        Ticket testPr3 = new Ticket("Word needs updating", 3, "C. Reporter", new Date());
        
        store.add(testPr1);
        store.add(testPr3);
        store.add(testPr5);
        
        assertTrue(sameOpenTicket(testPr1, store.getTicketById(testPr1.getTicketID())));
        assertTrue(sameOpenTicket(testPr3, store.getTicketById(testPr3.getTicketID())));
        assertTrue(sameOpenTicket(testPr5, store.getTicketById(testPr5.getTicketID())));
    }
    
    
    @Test
    public void testTicketStoreGetTicketByIDNotInStore() throws Exception {
        
        TicketStore store = new TicketStore(TestConfig.TEST_DB_URI);
        
        assertNull(store.getTicketById(0));  // not valid
        assertNull(store.getTicketById(-2));  // not valid
        assertNull(store.getTicketById(2000000));  // doesn't exist valid
        
    }
}