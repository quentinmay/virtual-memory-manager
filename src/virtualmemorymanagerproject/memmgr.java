
package virtualmemorymanagerproject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import static java.lang.Integer.parseInt;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;


public class memmgr {
    
    int ARGC_ERROR = 1;
    int FILE_ERROR = 2;
    int BUFLEN = 256;
    int FRAME_SIZE = 256;
    
    byte[] main_mem = new byte[65536];
    char[] main_mem_fifo = new char[32768];
    int[] page_queue = new int[128];
    int qhead = 0, qtail = 0;
    int[][] tlb = new int[16][2];
    int current_tlb_entry = 0;
    int[] page_table = new int[256];
    
    int currentFrame = 0;
    int pageFaultCount = 0;
    int tlbHitCount = 0;
    int accessCount = 0;
    
    
    public void run() throws FileNotFoundException, IOException  {
        Arrays.fill(page_table, -1);  
        try {
            ArrayList<String> addresses = readFile(System.getProperty("user.dir") + "\\src\\virtualmemorymanagerproject\\addresses.txt");
            for (int i = 0; i < addresses.size();i++) {
                int physicalAddress = getPhysicalMemory(addresses.get(i));
                //After every address, it lists necessary info
                System.out.println("Logical: " + addresses.get(i) + " (Page: " + getPage(addresses.get(i)) + " Offset: " + getOffset(addresses.get(i)) + ") Physical address: " + physicalAddress
                                   + " -> value: " + main_mem[physicalAddress - 1] + " #hits: " + tlbHitCount);
                
            }
            //Running all addresses is complete. List statistics
            System.out.println("Page faults: " + pageFaultCount + " TLB hits: " + tlbHitCount + " Access count: " + accessCount);
            System.out.println("Page-fault rate: " + (double)(100 * (double)pageFaultCount / (double)accessCount) + "%");
            System.out.println("TLB hit rate: " + (double)(100 * (double)tlbHitCount / (double)accessCount) + "%");
            
            
            
        } catch (FileNotFoundException ex) {
            Logger.getLogger(memmgr.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
    
    //Only call updateTLB when we know we have a page fault, so we dont need to recheck the TLB if it has our new entry.
    public void updateTLB(int pageNumber, int frame) {
        if (current_tlb_entry == 16) current_tlb_entry = 0; //Reset tlb to start at top again
        tlb[current_tlb_entry] = new int[]{pageNumber, frame};
        current_tlb_entry++;
    }
    
    //Converts the virtual memory address to physicalAddress. Does necessary paging if necessary.
    public int getPhysicalMemory(String address) throws IOException {
        accessCount++;
        int physicalAddress = -1;
        int pageNumber = getPage(address);
        int offset = getOffset(address);
        
        int frameNumber = checkTlb(pageNumber);
        
        if (frameNumber != -1) { //TLB hit
            physicalAddress = (frameNumber * FRAME_SIZE) + offset;
            tlbHitCount++;
            
            
        } else { //TLB miss. Now check page table
            
            if (page_table[pageNumber] == -1) { //Page table miss. Means we have a page fault.
                
                byte[] page = getPageFromBin(pageNumber);
                //now need to add this page to main_mem at the spot of current_frame
                int physicalAddressStartFrame = (currentFrame * FRAME_SIZE);
                for (int i = 0; i < page.length; i++) {
                   main_mem[physicalAddressStartFrame + i] = page[i];
                }
                
                physicalAddress = (currentFrame * FRAME_SIZE) + offset;
                
                updateTLB(pageNumber, currentFrame);
                
                //Update page table too
                page_table[pageNumber] = currentFrame;
                
                currentFrame = (currentFrame + 1) % FRAME_SIZE;
                
            } else { //page table hit.
                frameNumber = page_table[pageNumber];
                physicalAddress = (frameNumber * FRAME_SIZE) + offset;
                updateTLB(pageNumber, frameNumber);
                
            }
            
        }
        
        return physicalAddress;
        
        
        
        
    }

    
    //This reads the page from the binary file when given a page number to find.
    public byte[] getPageFromBin(int pageNumber) throws FileNotFoundException, IOException {
        FileInputStream fis = new FileInputStream(new File(System.getProperty("user.dir") + "\\src\\virtualmemorymanagerproject\\BACKING_STORE.bin"));
        int start = pageNumber * 256; //Start this many bytes in.
        int end = start + 256;
        byte[] page = new byte[256];
        int index = 0;
        for (int i =0; i < end; i++) { //count all the way up till end of desired page
            byte data = (byte) fis.read();
            if (i > start && i < end) {
                page[index] = data;
                index++;
            }
        }
        fis.close();
        pageFaultCount++;
        return page;
    }
    
    //Just checks to see if our page is in the TLB. Returns -1 if not.
    public int checkTlb(int pageNumber) {
        for (int i = 0; i < tlb.length; i++) {
            if (tlb[i][0] == pageNumber) {
                return tlb[i][1]; //TLB hit. Return frame number.
            }
        }
        return -1; //TLB miss. Return -1 indicating its a miss.
    }
    
    public ArrayList<String> readFile(String filePath) throws FileNotFoundException{
        File file = new File(filePath);
        Scanner reader = new Scanner(file);
        ArrayList<String> stringArr = new ArrayList<String>();
        while (reader.hasNextLine()) {
            String line = reader.nextLine();
            if (line.isBlank()) continue;
            stringArr.add(line);
        }
        return stringArr;
    }
    
    
    public int getPage(String address) {
        int input = parseInt(address);
        return (input >>> 8) & 0b11111111;
    }
    public int getOffset(String address) {
        int input = parseInt(address);
        return input & 0b11111111;
    }
}
