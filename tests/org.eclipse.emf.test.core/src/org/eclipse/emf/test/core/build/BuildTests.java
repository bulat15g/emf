/**
 * <copyright>
 *
 * Copyright (c) 2002-2004 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *   IBM - Initial API and implementation
 *
 * </copyright>
 *
 * $Id: BuildTests.java,v 1.1 2004/03/06 17:31:33 marcelop Exp $
 */
package org.eclipse.emf.test.core.build;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.zip.ZipFile;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.core.boot.BootLoader;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.service.environment.Constants;

public class BuildTests extends TestCase
{
    private List copyrightExcludeDirectories;
    private List cvsExcludeDirectories;
    private String sourceDirectoryName;
    private String logFileName;
    private static final String CVS_KO = "-ko";
    private static final String CVS_KKV = "-kkv";
    private static final String CVS_KB = "-kb";
    private static final String CVS_BINARY = "CVS_BINARY";
    private static final int ENTRY_TYPE_INDEX = 3;
    private static final int ENTRY_NAME_INDEX = 0;
    private static final int MIN_ENTRY_FIELDS_SIZE = 4;
    private static final String DEFAULT_CVS_TYPE = "-kkv";
    private Map cvsTypes;
    private List cvsDirectoryTypes;
    private String[] javaCopyrightLines;
    private long goodCopyrights = 0;
    private long badCopyrights = 0;
    private static final int HTML = 0;
    private static final int PROPERTIES = 1;
    private static final int XML = 2;

    private static FileTool.IZipFilter getTrueFilter()
    {
        return new FileTool.IZipFilter()
        {
            public boolean shouldExtract(String fullEntryName, String entryName, int depth)
            {
                return true;
            }

            public boolean shouldUnzip(String fullEntryName, String entryName, int depth)
            {
                return true;
            }
        };
    }

    /**
     * Method hasErrors.
     * 
     * @param string
     * @return boolean
     */
    private boolean hasErrors(String string)
    {
        boolean result = false;
        BufferedReader aReader = null;
        try
        {
            aReader = new BufferedReader(new InputStreamReader(new FileInputStream(string)));
            String aLine = aReader.readLine();
            while(aLine != null)
            {
                int aNumber = parseLine(aLine);
                if(aNumber > 0)
                {
                    result = true;
                }
                aLine = aReader.readLine();
            }
        }
        catch(FileNotFoundException e)
        {
            System.out.println("Could not open log file: " + string);
            result = true;
        }
        catch(IOException e)
        {
            System.out.println("Error reading log file: " + string);
            result = true;
        }
        finally
        {
            if(aReader != null)
            {
                try
                {
                    aReader.close();
                }
                catch(IOException e)
                {
                    result = true;
                }
            }
        }
        return result;
    }

    public void bogusTestCopyright()
    { // Not ready for primetime yet.
        boolean result = false;
        initializeJavaCopyright();
        new File(logFileName).mkdirs();
        logFileName = logFileName + File.separator + "copyrightLog.txt";
        try
        {
            BufferedWriter aLog = new BufferedWriter(new FileWriter(logFileName));
            File rootDirectory = new File(sourceDirectoryName);
            result = scanCopyrights(rootDirectory, aLog);
            aLog.close();
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
        System.out.println("Total copyrights: " + (goodCopyrights + badCopyrights));
        System.out.println("Good copyrights: " + goodCopyrights);
        System.out.println("Bad copyrights: " + badCopyrights);
        assertFalse("Copyright errors.  See the releng test logs linked from the test results page for details.", result);
    }

    /**
     * @param rootDirectory
     * @param aLog
     * @return boolean
     */
    private boolean scanCopyrights(File aDirectory, BufferedWriter aLog)
    {
        boolean result = false;
        if(isCopyrightExcludeDirectory(aDirectory))
        {
            return result;
        }
        File[] files = aDirectory.listFiles();
        if(files == null)
        {
            return result;
        }
        for(int i = 0; i < files.length; i++)
        {
            File aFile = files[i];
            if(aFile.isDirectory())
            {
                result = result | scanCopyrights(aFile, aLog);
            }
            else
            {
                result = result | validateCopyright(aFile, aLog);
            }
        }
        return result;
    }

    public void ztestCVSKTag()
    {
        boolean result = false;
        initializeCVSTypes();
        new File(logFileName).mkdirs();
        logFileName = logFileName + File.separator + "cvsTypesLog.txt";
        try
        {
            BufferedWriter aLog = new BufferedWriter(new FileWriter(logFileName));
            File rootDirectory = new File(sourceDirectoryName);
            result = scanCVSKTag(rootDirectory, aLog);
            aLog.close();
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
        assertFalse("CVS KTag type errors.  See the releng test logs linked from the test results page for details.", result);
    }

    private void initializeCVSTypes()
    {
        cvsTypes = new HashMap();
        cvsDirectoryTypes = new ArrayList();
        cvsExcludeDirectories = new ArrayList();
        cvsTypes.put("gif", CVS_BINARY);
        cvsTypes.put("jpg", CVS_BINARY);
        cvsTypes.put("zip", CVS_BINARY);
        cvsTypes.put("jar", CVS_BINARY);
        cvsTypes.put("bmp", CVS_BINARY);
        cvsTypes.put("class", CVS_BINARY);
        cvsTypes.put("dll", CVS_BINARY);
        cvsTypes.put("doc", CVS_BINARY);
        cvsTypes.put("exe", CVS_BINARY);
        cvsTypes.put("ico", CVS_BINARY);
        cvsTypes.put("jpeg", CVS_BINARY);
        cvsTypes.put("pdf", CVS_BINARY);
        cvsTypes.put("png", CVS_BINARY);
        cvsTypes.put("ppt", CVS_BINARY);
        cvsTypes.put("so", CVS_BINARY);
        cvsTypes.put("tiff", CVS_BINARY);
        cvsTypes.put("tif", CVS_BINARY);
        cvsTypes.put("xls", CVS_BINARY);
        cvsTypes.put("rsc", CVS_BINARY);
        cvsTypes.put("jnilib", CVS_BINARY);
        cvsTypes.put("a", CVS_BINARY);
        cvsTypes.put("sl", CVS_BINARY);
        cvsTypes.put("a", CVS_BINARY);
        cvsTypes.put("1", CVS_BINARY);
        cvsTypes.put("xpm", CVS_BINARY);
        cvsTypes.put("a", CVS_BINARY);
        cvsTypes.put("pm", CVS_BINARY);
        // Define directories with all binary types
        cvsDirectoryTypes.add("org.eclipse.jdt.ui.tests.refactoring" + File.separator + "resources");
        // Define exclude directories
        cvsExcludeDirectories.add("org.eclipse.jdt.ui.tests.refactoring" + File.separator + "resources");
        cvsExcludeDirectories.add("platform-launcher");
    }

    private void initializeJavaCopyright()
    {
        copyrightExcludeDirectories = new ArrayList();
        javaCopyrightLines = new String[8];
        javaCopyrightLines[0] = "/*******************************************************************************";
        javaCopyrightLines[1] = " * Copyright (c) 2000, 2003 IBM Corporation and others.";
        javaCopyrightLines[2] = " * All rights reserved. This program and the accompanying materials ";
        javaCopyrightLines[3] = " * are made available under the terms of the Common Public License v1.0";
        javaCopyrightLines[4] = " * which accompanies this distribution, and is available at";
        javaCopyrightLines[5] = " * http://www.eclipse.org/legal/cpl-v10.html";
        javaCopyrightLines[6] = " * ";
        javaCopyrightLines[7] = " * Contributors:";
        // Define directories with all binary types
        copyrightExcludeDirectories.add("org.eclipse.jdt.ui.tests.refactoring" + File.separator + "resources");
        copyrightExcludeDirectories.add("org.eclipse.jdt.debug.tests" + File.separator + "resources");
    }

    private boolean scanCVSKTag(File aDirectory, BufferedWriter aLog)
    {
        boolean result = false;
        if(isCvsExcludeDirectory(aDirectory))
        {
            return result;
        }
        File[] files = aDirectory.listFiles();
        if(files == null)
        {
            return result;
        }
        for(int i = 0; i < files.length; i++)
        {
            File aFile = files[i];
            if(aFile.isDirectory())
            {
                if(aFile.getName().equals("CVS"))
                {
                    result = result | scanCVSDirectory(aFile, aLog);
                }
                else
                {
                    result = result | scanCVSKTag(aFile, aLog);
                }
            }
        }
        return result;
    }

    /**
     * @param aDirectory
     * @return
     */
    private boolean isCvsExcludeDirectory(File aDirectory)
    {
        String aString = aDirectory.getPath().substring(sourceDirectoryName.length()).toLowerCase();
        Iterator anIterator = cvsExcludeDirectories.iterator();
        while(anIterator.hasNext())
        {
            String anItem = (String)anIterator.next();
            if(aString.indexOf(anItem) != -1)
            {
                return true;
            }
        }
        return false;
    }

    /**
     * @param aDirectory
     * @return
     */
    private boolean isCopyrightExcludeDirectory(File aDirectory)
    {
        String aString = aDirectory.getPath().substring(sourceDirectoryName.length()).toLowerCase();
        Iterator anIterator = copyrightExcludeDirectories.iterator();
        while(anIterator.hasNext())
        {
            String anItem = (String)anIterator.next();
            if(aString.indexOf(anItem) != -1)
            {
                return true;
            }
        }
        return false;
    }

    private boolean scanCVSDirectory(File aDirectory, BufferedWriter aLog)
    {
        boolean result = false;
        File entries = new File(aDirectory, "Entries");
        try
        {
            BufferedReader aReader = new BufferedReader(new FileReader(entries));
            String aLine = aReader.readLine();
            while(aLine != null)
            {
                result = result | validateCVSEntry(aDirectory.getParentFile(), aLine, aLog);
                aLine = aReader.readLine();
            }
            aReader.close();
        }
        catch(FileNotFoundException e)
        {
            System.out.println("File Not Found reading Entries file");
            e.printStackTrace();
        }
        catch(IOException e)
        {
            System.out.println("IOException reading Entries file");
            e.printStackTrace();
        }
        return result;
    }

    /**
     * @param aFile
     * @return boolean - true if the copyright is NOT valid.
     */
    private boolean validateCopyright(File aFile, BufferedWriter aLog)
    {
        boolean result = false;
        if(!aFile.getName().endsWith(".java"))
        {
            return result;
        }
        LineNumberReader aReader;
        try
        {
            aReader = new LineNumberReader(new FileReader(aFile));
            for(int i = 0; i < javaCopyrightLines.length; i++)
            {
                String aLine = aReader.readLine();
                if(aLine == null)
                {
                    result = true;
                    break;
                }
                if(i > -1)
                {
                    if(!aLine.trim().toLowerCase().equals(javaCopyrightLines[i].trim().toLowerCase()))
                    {
                        result = true;
                        break;
                    }
                }
            }
            aReader.close();
        }
        catch(FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
        if(result)
        {
            badCopyrights++;
            try
            {
                int start = aFile.getPath().indexOf("plugins");
                String fileName;
                if(start == -1)
                {
                    fileName = aFile.getPath();
                }
                else
                {
                    fileName = aFile.getPath().substring(start + "plugins".length() + 1);
                }
                aLog.write(fileName);
                aLog.newLine();
            }
            catch(IOException e1)
            {
                e1.printStackTrace();
            }
        }
        else
        {
            goodCopyrights++;
        }
        return result;
    }

    private boolean validateCVSEntry(File aDirectory, String aLine, BufferedWriter aLog)
    {
        boolean result = false;
        String[] fields = split(aLine, "/");
        if(fields.length < MIN_ENTRY_FIELDS_SIZE)
        {
            return result;
        }
        String expectedType;
        // Some entire directories are marked as Binary.
        if(isCVSBinaryDirectory(aDirectory))
        {
            expectedType = CVS_BINARY;
        }
        else
        {
            expectedType = null;
        }
        String entryName = aDirectory + File.separator + fields[ENTRY_NAME_INDEX];
        if(expectedType == null)
        {
            expectedType = (String)cvsTypes.get(entryName);
        }
        // No type registered for exact file name. Check for extension
        if(expectedType == null)
        {
            String[] dotParts = split(fields[ENTRY_NAME_INDEX], ".");
            String entryExtension;
            if(dotParts.length == 0)
            {
                // File name has no extension.
                expectedType = DEFAULT_CVS_TYPE;
            }
            else
            {
                entryExtension = dotParts[dotParts.length - 1];
                expectedType = (String)cvsTypes.get(entryExtension);
                if(expectedType == null)
                {
                    // Extension has no type registered
                    expectedType = DEFAULT_CVS_TYPE;
                }
            }
        }
        // We know what type to expect for this file. Are we the right one?
        String entryType = fields[ENTRY_TYPE_INDEX];
        if(entryType.length() == 0)
        {
            entryType = CVS_KKV;
        }
        try
        {
            if(expectedType.equals(CVS_BINARY))
            {
                if(!entryType.equals(CVS_KB))
                {
                    // Fail Binary Test
                    int start = entryName.indexOf("plugins");
                    String fileName;
                    if(start == -1)
                    {
                        fileName = entryName;
                    }
                    else
                    {
                        fileName = entryName.substring(start + "plugins".length() + 1);
                    }
                    aLog.write(fileName + " should be *BINARY*");
                    aLog.newLine();
                    result = true;
                }
            }
            else
            {
                if(!(entryType.equals(CVS_KKV) || entryType.equals(CVS_KO)))
                {
                    // Fail
                    int start = entryName.indexOf("plugins");
                    String fileName;
                    if(start == -1)
                    {
                        fileName = entryName;
                    }
                    else
                    {
                        fileName = entryName.substring(start + "plugins".length() + 1);
                    }
                    aLog.write(fileName + " should be *TEXT*");
                    aLog.newLine();
                    result = true;
                }
            }
        }
        catch(IOException e)
        {
            System.out.println("IOException writing log");
            e.printStackTrace();
        }
        return result;
    }

    /**
     * @param aDirectory
     * @return
     */
    private boolean isCVSBinaryDirectory(File aDirectory)
    {
        String aString = aDirectory.getPath().substring(sourceDirectoryName.length()).toLowerCase();
        Iterator anIterator = cvsDirectoryTypes.iterator();
        while(anIterator.hasNext())
        {
            String anItem = (String)anIterator.next();
            if(aString.indexOf(anItem) != -1)
            {
                return true;
            }
        }
        return false;
    }

    /**
     * @param aLine
     * @param delimeter
     * @return String[]
     */
    private String[] split(String aLine, String delimeter)
    {
        StringTokenizer tokenizer = new StringTokenizer(aLine, delimeter, true);
        List list = new ArrayList();
        String lastToken = "";
        while(tokenizer.hasMoreTokens())
        {
            String aToken = tokenizer.nextToken();
            if(aToken.equals(delimeter))
            {
                if(lastToken.equals(delimeter))
                {
                    list.add("");
                }
            }
            else
            {
                list.add(aToken);
            }
            lastToken = aToken;
        }
        return (String[])list.toArray(new String[0]);
    }

    public void testChkpii()
    {
        String[] zipFiles = locateBuildGeneratedZipFiles();
        String sniffFolder = Platform.getInstanceLocation().toOSString();
        FileTool.IZipFilter zipFilter = getTrueFilter();
        
        for (int i = 0; i < zipFiles.length; i++)
        {
            try
            {
                FileTool.unzip(zipFilter, new ZipFile(zipFiles[i]), new File(sniffFolder));
            }
            catch(IOException e)
            {
                fail(zipFiles[i] + ": " + sniffFolder + ": " + "IOException unzipping Eclipse for chkpii");
            }           
        }

        boolean result1 = testChkpii(HTML);
        boolean result2 = testChkpii(XML);
        boolean result3 = testChkpii(PROPERTIES);
        assertTrue("Translation errors in files.  See the chkpii logs linked from the test results page for details.", (result1 && result2 && result3));
    }

    private boolean testChkpii(int type)
    {
        Runtime aRuntime = Runtime.getRuntime();
        String chkpiiString = getChkpiiString(type);
        try
        {
            Process aProcess = aRuntime.exec(chkpiiString);
            BufferedReader aBufferedReader = new BufferedReader(new InputStreamReader(aProcess.getInputStream()));
            String line = null;
            while((line = aBufferedReader.readLine()) != null)
            {
            }
            aProcess.waitFor();
        }
        catch(IOException e)
        {
            e.printStackTrace();
            return false;
        }
        catch(InterruptedException e)
        {
            return false;
        }
        return !hasErrors(getOutputFile(type));
    }
    
    private String adjustPath(String path)
    {
        if(path == null)
            return null;
        
        if(File.separatorChar == '\\')
            path = path.replace('/', File.separatorChar);
        else if(File.separatorChar == '/')
            path = path.replace('\\', File.separatorChar);        
        return path;    
    }

    /**
     * Method getChkpiiString.
     * 
     * @param HTML
     * @return String
     */
    private String getChkpiiString(int type)
    {
        return adjustPath(getExec() + " " + getFilesToTest(type) + " -E -O " + getOutputFile(type) + getExcludeErrors() + getExcludeFile() + " -S");
    }

    /**
     * Method locateEclipseZip.
     * 
     * @return String
     */
    private String[] locateBuildGeneratedZipFiles()
    {
        // String to use when running as an automated test.
        String installDir = BootLoader.getInstallURL().getPath() + File.separator + ".." + File.separator + "..";
        
        // String to use when running in Eclipse
        // String installDir = BootLoader.getInstallURL().getPath() + "..";
        
        try
        {
            installDir = adjustPath(new File(installDir).getCanonicalPath().toString());
        }
        catch (IOException e)
        {
        }
        File aFile = new File(installDir);
        

        List zipFiles = new ArrayList();
        File[] files = aFile.listFiles();
        for(int i = 0; i < files.length; i++)
        {
            File file = files[i];
            String fileName = file.getName();
            
            if(fileName.endsWith(".zip"))
            {
                for (int j = 0; j < BUILD_GENERATED_ZIP_FILES_SUFFIX.length; j++)
                {
                    if(fileName.startsWith(BUILD_GENERATED_ZIP_FILES_SUFFIX[j]))
                        zipFiles.add(file.getAbsolutePath());
                }
            }
        }
        return (String[])zipFiles.toArray(new String[zipFiles.size()]);
    }

    /**
     * Method getExcludeFiles.
     * 
     * @return String
     */
    private String getExcludeFile()
    {
        String file = adjustPath(System.getProperty("PLUGIN_PATH")) + File.separator + "ignoreFiles.txt";
        if(new File(file).isFile())
            return " -X " + file;
        return "";
    }

    /**
     * Method getOutputFile.
     * 
     * @param HTML
     * @return String
     */
    private String getOutputFile(int type)
    {
        new File(logFileName).mkdirs();
        String aString = logFileName + File.separator + "org.eclipse.nls.";
        aString = new File(aString).getPath();
        switch(type)
        {
            case HTML:
                return aString + "html.txt";
            case PROPERTIES:
                return aString + "properties.txt";
            case XML:
                return aString + "xml.txt";
            default:
                return aString + "other.txt";
        }
    }

    /**
     * Method getFilesToTest.
     * 
     * @param HTML
     * @return String
     */
    private String getFilesToTest(int type)
    {
        String sniffFolder = Platform.getInstanceLocation().toOSString();
        String aString = new File(sniffFolder).getPath() + File.separator;
        switch(type)
        {
            case HTML:
                return aString + "*.htm*";
            case PROPERTIES:
                return aString + "*.properties";
            case XML:
                return aString + "*.xml";
            default:
                return aString + "*.*";
        }
    }

    /**
     * Method getExec.
     * 
     * @return String
     */
    private String getExec()
    {
        return new File("chkpii.exe").getPath();
    }

    /**
     * Method getExcludeErrors.
     */
    private String getExcludeErrors()
    {
        String os = BootLoader.getOS();
        String fileName;
        if(os.equals(Constants.OS_WIN32))
        {
            fileName = "ignoreErrorsWindows.txt";
        }
        else
        {
            fileName = "ignoreErrorsUnix.txt";
        }
        String aString = System.getProperty("PLUGIN_PATH");
        aString = adjustPath(aString) + File.separator + fileName;
        if(new File(aString).isFile())
            return " -XM @" + aString;
        
        return "";
    }

    /**
     * Method parseLine.
     * 
     * @param aLine
     * @return -1 if not an error or warning line or the number of errors or
     *         warnings.
     */
    private int parseLine(String aLine)
    {
        int index = aLine.indexOf("Files Could Not Be Processed: ");
        if(index == -1)
        {
            index = aLine.indexOf("Files Contain Error");
        }
        if(index == -1)
        {
            return -1;
        }
        else
        {
            String aString = aLine.substring(0, index).trim();
            return Integer.parseInt(aString);
        }
    }

    public class FileSuffixFilter implements FilenameFilter
    {
        private String suffix;

        public FileSuffixFilter(String suffix)
        {
            this.suffix = suffix;
        }

        /**
         * @see java.io.FilenameFilter#accept(java.io.File, java.lang.String)
         */
        public boolean accept(File dir, String name)
        {
            int lastDot = name.lastIndexOf('.');
            if(lastDot == -1)
            {
                return false;
            }
            return name.substring(lastDot).equals(suffix);
        }
    }
    
    private boolean isValidFeature(File feature)
    {
        return isValidPlugin(feature);
    }

    private boolean isValidPlugin(File plugin)
    {
        if(plugin != null)
        {
            String name = plugin.getName();
            if(name.startsWith("org.eclipse.emf") || name.startsWith("org.eclipse.xsd"))
                return (name.indexOf("emf.test") < 0);
        }
        
        return false;
    }

    public static final String[] BUILD_GENERATED_ZIP_FILES_SUFFIX =
    {
            "emf-runtime", "emf-source", "emf-doc",
            "xsd-runtime", "xsd-source", "xsd-doc",
            "sdo-runtime", "sdo-source", "sdo-doc",
            "emf-xsd-sdo-SDK"
    };
    
    public static final String[] REQUIRED_FEATURE_FILES = {"cpl-v10.html", "feature.properties", "feature.xml", "license.html"};
    public static final String REQUIRED_FEATURE_SUFFIX = ".jpg";
    public static final String[] REQUIRED_PLUGIN_FILES = {"about.html", "plugin.properties", "plugin.xml"};
    public static final String REQUIRED_PLUGIN_SUFFIX = ".jar";
    public static final String[] REQUIRED_FEATURE_PLUGIN_FILES = {"about.html", "about.ini", "about.mappings", "about.properties", "plugin.properties",
            "plugin.xml"};
    public static final String REQUIRED_FEATURE_PLUGIN_SUFFIX = ".gif";
    public static final String[] REQUIRED_FRAGMENT_FILES = {"fragment.xml"};
    public static final String REQUIRED_FRAGMENT_SUFFIX = "";
    public static final String[] REQUIRED_SOURCE_FILES = {"about.html"};
    public static final String REQUIRED_SOURCE_SUFFIX = ".zip";
    public static final String[] SUFFIX_EXEMPT_LIST = {"org.eclipse.swt", "org.apache.ant"};
    public static final int PLUGIN_COUNT = 84; // - 20; // Note this number
                                               // must include non-shipping
                                               // test plugins
    public static final int FEATURE_COUNT = 9; // - 1; // Note this number must
                                               // include non-shipping test
                                               // feature

    /**
     * Constructor for EmptyDirectoriesTest.
     * 
     * @param arg0
     */
    public BuildTests(String arg0)
    {
        super(arg0);
    }
    
    public static Test suite()
    {
        TestSuite ts = new TestSuite();
        ts.addTest(new BuildTests("testFeatureFiles"));
        ts.addTest(new BuildTests("testPluginFiles"));
        return ts;
    }    

    /**
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        assertNotNull(getExec());
        
        // Autoamted Test
        logFileName = BootLoader.getInstallURL().getPath() + ".." + File.separator + ".." + File.separator + "results" + File.separator + "chkpii"; // A
                                                                                                                                                    // tad
                                                                                                                                                    // bogus
                                                                                                                                                    // but
                                                                                                                                                    // this
                                                                                                                                                    // is
                                                                                                                                                    // where
                                                                                                                                                    // the
                                                                                                                                                    // build
                                                                                                                                                    // wants
                                                                                                                                                    // to
                                                                                                                                                    // copy
                                                                                                                                                    // the
                                                                                                                                                    // results
                                                                                                                                                    // from!
        
        sourceDirectoryName = BootLoader.getInstallURL().getPath() + ".." + File.separator + ".." + File.separator + ".." + File.separator + ".."
                + File.separator + "src";
        // Runtime Workbench - TODI Put me back to Automated status
        //      logFileName = "d:\\results";
        //      sourceDirectoryName = "d:\\sourceFetch";

        try
        {
            logFileName = adjustPath(new File(logFileName).getCanonicalPath().toString());
        }
        catch (IOException e)
        {
        }
        try
        {
            sourceDirectoryName = adjustPath(new File(sourceDirectoryName).getCanonicalPath().toString());
        }
        catch (IOException e)
        {
        }
        
        System.setProperty("PLUGIN_PATH", adjustPath(BootLoader.getInstallURL().getPath()) + "plugins");
    }

    
    public void testFeatureFiles()
    {
        List result = new ArrayList();
        String installDir = BootLoader.getInstallURL().getPath();
        File featureDir = new File(installDir, "features");
        File[] features = featureDir.listFiles();
        for(int i = 0; i < features.length; i++)
        {
            File aFeature = features[i];
            if(!isValidFeature(aFeature))
                continue;
            
            if(!testDirectory(aFeature, REQUIRED_FEATURE_FILES, REQUIRED_FEATURE_SUFFIX))
            {
                result.add(aFeature.getPath());
            }
        }
        String aString = "";
        if(result.size() > 0)
        {
            Iterator iter = result.iterator();
            while(iter.hasNext())
            {
                String element = (String)iter.next();
                aString = aString + element + "; ";
            }
        }
        assertTrue("Feature directory missing required files: " + aString, result.size() == 0);
    }

    //  public void testPluginCount() {
    //      String installDir = BootLoader.getInstallURL().getPath();
    //      File pluginDir = new File(installDir, "plugins");
    //      File[] plugins = pluginDir.listFiles();
    //
    //      assertTrue("Plug-ins missing: " + (PLUGIN_COUNT - plugins.length),
    // PLUGIN_COUNT == plugins.length);
    //  }
    //  public void testFeatureCount() {
    //      String installDir = BootLoader.getInstallURL().getPath();
    //      File featureDir = new File(installDir, "features");
    //      File[] features = featureDir.listFiles();
    //
    //      assertTrue("Features missing: " + (FEATURE_COUNT - features.length),
    // FEATURE_COUNT == features.length);
    //  }
    //  
    public void testPluginFiles()
    {
        List result = new ArrayList();
        String installDir = BootLoader.getInstallURL().getPath();
        File pluginDir = new File(installDir, "plugins");
        File[] plugins = pluginDir.listFiles();
        for(int i = 0; i < plugins.length; i++)
        {
            File aPlugin = plugins[i];
            if(!isValidFeature(aPlugin))
                continue;

            if(!testPluginFile(aPlugin))
            {
                result.add(aPlugin.getPath());
            }
        }
        String aString = "";
        if(result.size() > 0)
        {
            Iterator iter = result.iterator();
            while(iter.hasNext())
            {
                String element = (String)iter.next();
                aString = aString + element + "; ";
            }
        }
        assertTrue("Plugin directory missing required files: " + aString, result.size() == 0);
    }

    private boolean testPluginFile(File aPlugin)
    {
        // Are we a doc plugin?
        if(testDirectory(aPlugin, REQUIRED_PLUGIN_FILES, ".zip"))
        {
            return true;
        }
        // Are we a feature plugin?
        if(testDirectory(aPlugin, REQUIRED_FEATURE_PLUGIN_FILES, REQUIRED_FEATURE_PLUGIN_SUFFIX))
        {
            return true;
        }
        // Are we a regular plugin
        if(testDirectory(aPlugin, REQUIRED_PLUGIN_FILES, REQUIRED_PLUGIN_SUFFIX))
        {
            return true;
        }
        // Are we a source plugin
        if(testSourcePlugin(aPlugin))
        {
            return true;
        }
        // Are we a fragment
        if(testDirectory(aPlugin, REQUIRED_FRAGMENT_FILES, REQUIRED_FRAGMENT_SUFFIX))
        {
            return true;
        }
        // No then we are bad
        return false;
    }

    private boolean testDirectory(File aDirectory, String[] requiredFiles, String requiredSuffix)
    {
        if(!Arrays.asList(aDirectory.list()).containsAll(Arrays.asList(requiredFiles)))
        {
            return false;
        }
        int index = aDirectory.getName().indexOf('_');
        if(index == -1)
        {
            index = aDirectory.getName().length();
        }
        String plainName = aDirectory.getName().substring(0, index);
        if(requiredSuffix.equals("") || Arrays.asList(SUFFIX_EXEMPT_LIST).contains(plainName))
        {
            return true;
        }
        else if(aDirectory.listFiles(new FileSuffixFilter(requiredSuffix)).length != 0)
        {
            return true;
        }
        else
        {
            aDirectory = new File(aDirectory, "runtime");
            if((aDirectory.isDirectory()) && (aDirectory.listFiles(new FileSuffixFilter(requiredSuffix)).length != 0))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Return true if the receiver is a source plugin, false otherwise A
     * separate method because this is a little tricky.
     * 
     * @param aPlugin
     * @return boolean
     */
    private boolean testSourcePlugin(File aPlugin)
    {
        if(!testDirectory(aPlugin, REQUIRED_PLUGIN_FILES, ""))
        {
            return false;
        }
        File sourceDir = new File(aPlugin, "src");
        File[] sourceDirs = sourceDir.listFiles();
        if(sourceDirs == null)
        {
            return false;
        }
        for(int i = 0; i < sourceDirs.length; i++)
        {
            File aSourceDir = sourceDirs[i];
            if(!testDirectory(aSourceDir, REQUIRED_SOURCE_FILES, REQUIRED_SOURCE_SUFFIX))
            {
                return false;
            }
        }
        return true;
    }
}