/*
 * MainINSECTForm.java
 *
 * Created on 24 Ιανουάριος 2006, 10:36 μμ
 */

package gr.demokritos.iit.jinsect.gui;

import gr.demokritos.iit.conceptualIndex.structs.Distribution;
import java.awt.event.ItemEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import gr.demokritos.iit.jinsect.structs.CategorizedFileEntry;
import gr.demokritos.iit.jinsect.classification.Decider;
import gr.demokritos.iit.jinsect.structs.DocumentSet;
import gr.demokritos.iit.jinsect.classification.Suggester;
//import gr.demokritos.iit.jinsect.documentModel.documentTypes.SimpleTextCategory;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramGraph;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramSymWinGraph;
import gr.demokritos.iit.jinsect.indexing.NamedDocumentNGramGraph;
import gr.demokritos.iit.jinsect.storage.INSECTFileDB;
import gr.demokritos.iit.jinsect.storage.INSECTMemoryDB;
import java.util.Arrays;
import gr.demokritos.iit.jinsect.structs.Decision;
import gr.demokritos.iit.jinsect.threading.ThreadQueue;
import gr.demokritos.iit.jinsect.utils;
import java.awt.event.WindowAdapter;
import java.util.Hashtable;

/**
 *
 * @author  PCKid
 */
public class MainINSECTForm extends javax.swing.JFrame {
    INSECTMemoryDB DataRepository;
    Suggester CategorySuggester;
    Decider CategoryDecider;
    //SVMDecider CategoryDecider;
    //SVMDocDecider CategoryDecider;
    
    boolean bStopFold = false;
    
    // Constants
    public static final String NO_CATEGORY = "<Καμία Κατηγορία>";
    public static final String NEW_CATEGORY = "<Νέα Κατηγορία>";
    
    public final int EXIT_ON_CLOSE = 1;
    //private String[] Categories;
    private StatusFrame sfStatus;

    public static void main(String[] saArgs) {
        MainINSECTForm m = new MainINSECTForm();

        Hashtable<String,String> hSwitches = utils.parseCommandLineSwitches(saArgs);
        m.DataDirEd.setText(utils.getSwitch(hSwitches, "data", "./models"));
        m.TrainingSetDirEd.setText(utils.getSwitch(hSwitches, "train", ""));
        m.UseTrainingDirChk.setSelected(utils.getSwitch(hSwitches, 
                "train", "").length() > 0);
        m.CorpusDirEd.setText(utils.getSwitch(hSwitches, "test", "./test"));
        m.setVisible(true);
        //m.SecondaryStatusBar.setVisible(false);

        m.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                super.windowClosed(e);

                System.exit(0);
            }
 });
    }
    
    /** Creates new form MainINSECTForm */
    public MainINSECTForm() {
        initComponents();
        
        DataRepository = new INSECTMemoryDB();
        CategorySuggester = new Suggester();
        //CategoryDecider = new Decider(DataRepository);
        // Try SVM
        CategoryDecider = new Decider(DataRepository) {
            @Override
            protected final String filterDataString(String sStr,
                    NamedDocumentNGramGraph cCat) {
                return sStr;
            }
        };
        
        UpdateCategories();
        pack();
    }
    
    public void UpdateCategoriesByUIValues() {
        // Update existing categories
        CategoryList.setListData(DataRepository.getObjectList(
                INSECTMemoryDB.CATEGORY_TYPE));
    }
    
    public void UpdateCategories() {
        // Update existing categories
        //DataRepository = new INSECTFileDB();
        CategoryList.setListData(CategoryDecider.getAvailableCategories());
    }
    
    public String SelectCategory(String sSuggestedCategory) {
        String[] sAvailableCategories = new String[CategoryDecider.getAvailableCategories().length + 2];
        Iterator iIter = Arrays.asList(CategoryDecider.getAvailableCategories()).iterator();
        int iCnt = 0;
        while (iIter.hasNext()) {
            sAvailableCategories[iCnt] = (String)iIter.next();
            iCnt++;
        }
        sAvailableCategories[iCnt++] = NEW_CATEGORY;
        sAvailableCategories[iCnt] = NO_CATEGORY;
        
        int iSelected = JOptionPane.showOptionDialog(this, "Παρακαλώ επιλέξτε κατηγορία:", "Επιλογή κατηγορίας", 
                JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE, null, sAvailableCategories , sSuggestedCategory);
        String sFinalCategory = sAvailableCategories[iSelected];
        return sFinalCategory;
    }
    
    public boolean checkFile(String sFilename, String sFinalCategory, boolean bTrain) {
        // Init doc
        DocumentNGramGraph dTemp = new DocumentNGramSymWinGraph();
        try {
            dTemp.loadDataStringFromFile(sFilename);
        } catch (IOException ex) {
            Logger.getLogger(MainINSECTForm.class.getName()).log(Level.SEVERE, null, ex);
        }
        Decision dRes = new Decision(dTemp, sFinalCategory, 1.0, new HashMap());
        
        String sSuggestedCategory = NO_CATEGORY;
        // Get decider suggestion        
        //if (!bTrain) {
        		dRes = CategoryDecider.suggestCategory(sFilename);        
        		sSuggestedCategory = (String)dRes.FinalDecision;
        //}
        
        // If suggester selected
        if (SuggesterOnChk.getSelectedObjects() != null) {
            // Also get suggester suggestion
            Decision dSuggesterRes = CategorySuggester.suggest(dRes.DecisionEvidence);

            // Suggester suggestion gets first priority, as it includes the decision
            // from the decider
            if (dSuggesterRes.FinalDecision == null)
                sSuggestedCategory = NO_CATEGORY;
            else
                sSuggestedCategory = (String)dSuggesterRes.FinalDecision;
        }
        
        // If one should select the category
        if (sFinalCategory.equals(NEW_CATEGORY)) {
            sFinalCategory = SelectCategory(sSuggestedCategory);
            // If the category is a new one
            if (sFinalCategory.equals(NEW_CATEGORY)) {
                sFinalCategory = JOptionPane.showInputDialog(null,
                        "Παρακαλώ εισάγετε το όνομα της κατηγορίας:",
                        "Νέα Κατηγορία " +
                        String.valueOf(CategoryDecider.getAvailableCategories().length));
            }
        }        
                        
        // If training is on
        if (bTrain) {
            // Update decider
            UpdateSecondaryStatus("Updating decider", 0.0);                    
            if (!sFinalCategory.equals(NO_CATEGORY))
            {
                // Update only if a wrong decision would be made
                try {
                    if (!sSuggestedCategory.equalsIgnoreCase(sFinalCategory))
                        CategoryDecider.addEvidence(dRes, sFinalCategory);
                }
                catch (NullPointerException ne) {
                    CategoryDecider.addEvidence(dRes, sFinalCategory);
                    sSuggestedCategory = NO_CATEGORY;
                }
                UpdateCategories();
                
                // Update suggester
                UpdateSecondaryStatus("Updating suggester", 0.5);
                if ((!sSuggestedCategory.equals(NO_CATEGORY)) &&
                        (SuggesterOnChk.getSelectedObjects() != null)) {
                    // Inform suggester
                    CategorySuggester.train(dRes.DecisionEvidence, sSuggestedCategory, sFinalCategory);
                }
            }
            
            UpdateSecondaryStatus("Updating complete", 1.0);
        }
        if (sFinalCategory.equals(sSuggestedCategory)) {
            // DEBUG LINES
            System.out.println(String.format("Correctly decided '%s' with certainty %3.2f%%.\nEvidence: %s",
                    sSuggestedCategory, dRes.DecisionBelief * 100, 
                    dRes.DecisionEvidence.toString()));
            //////////////
            // Return result
            return true;
        }
        else
        {
            // DEBUG LINES
            System.out.println(String.format("Decided '%s' instead of '%s' with certainty %3.2f%%.\nEvidence: %s",
                    sSuggestedCategory, sFinalCategory, dRes.DecisionBelief * 100, 
                    dRes.DecisionEvidence.toString()));
            //////////////
            return false;
        }
    }
    
    public void UpdateStatus(String sText) {
        UpdateStatus(sText, -1);
    }
    
    public void UpdateStatus(final String sText, final double dPercent) {
        
        //if (sfStatus == null)
            //sfStatus = new StatusFrame();
        //if (!sfStatus.isVisible())
        //{
            //sfStatus.setVisible(true);
            //sfStatus.update(sfStatus.getGraphics());
        //}
        
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                String sTxt = sText;
                if (dPercent != -1)
                    sTxt = String.format("%3.2f%% - ", dPercent * 100).concat(sTxt);
                StatusBar.setText(sTxt);
                //update(getGraphics());
                Thread.yield();
                
                //sfStatus.setStatus(sTxt, dPercent);
            }
        }
        );
        
    }
    
    public void UpdateSecondaryStatus(String sText, double dPercent) {
        String sTxt = sText;
        if (dPercent != -1)
            sTxt = String.format("%3.2f%%  -\t ", dPercent * 100).concat(sTxt);
        SecondaryStatusBar.setText(sTxt);
        
        // Thread yield
        Thread.yield();        
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        OptionsMnu = new javax.swing.JPopupMenu();
        ShowSecondaryToolbarChk = new javax.swing.JCheckBoxMenuItem();
        StopProcessFoldMnu = new javax.swing.JMenuItem();
        jLabel1 = new javax.swing.JLabel();
        DataDirEd = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        CorpusDirEd = new javax.swing.JTextField();
        CategoryList = new javax.swing.JList();
        jLabel3 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        MainTestBtn = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();
        PruningFactorEd = new javax.swing.JTextField();
        TrainingPercentEd = new javax.swing.JTextField();
        AnalyzeFileBtn = new javax.swing.JButton();
        ClearCategoriesBtn = new javax.swing.JButton();
        jLabel6 = new javax.swing.JLabel();
        FoldsEd = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        PartOfCorpusEd = new javax.swing.JTextField();
        SuggesterOnChk = new javax.swing.JCheckBox();
        StatusBar = new javax.swing.JLabel();
        SecondaryStatusBar = new javax.swing.JLabel();
        FoldCountStatusBar = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        TrainingSetDirEd = new javax.swing.JTextField();
        UseTrainingDirChk = new javax.swing.JCheckBox();

        ShowSecondaryToolbarChk.setSelected(true);
        ShowSecondaryToolbarChk.setText("Show secondary toolbar");
        ShowSecondaryToolbarChk.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                ShowSecondaryToolbarChkItemStateChanged(evt);
            }
        });
        OptionsMnu.add(ShowSecondaryToolbarChk);

        StopProcessFoldMnu.setText("Stop process at end of fold");
        StopProcessFoldMnu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                StopProcessFoldMnuActionPerformed(evt);
            }
        });
        OptionsMnu.add(StopProcessFoldMnu);

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("JInsect v0.9");
        setFont(new java.awt.Font("Arial", 0, 10));
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });
        getContentPane().setLayout(new java.awt.GridBagLayout());

        jLabel1.setText("Data Directory:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        getContentPane().add(jLabel1, gridBagConstraints);

        DataDirEd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DataDirEdActionPerformed(evt);
            }
        });
        DataDirEd.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                DataDirEdMouseClicked(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        getContentPane().add(DataDirEd, gridBagConstraints);

        jLabel2.setText("Corpus Directory:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.ipadx = 9;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        getContentPane().add(jLabel2, gridBagConstraints);

        CorpusDirEd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CorpusDirEdActionPerformed(evt);
            }
        });
        CorpusDirEd.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                CorpusDirEdMouseClicked(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.ipadx = 344;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        getContentPane().add(CorpusDirEd, gridBagConstraints);

        CategoryList.setPreferredSize(new java.awt.Dimension(100, 100));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridheight = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.ipadx = 250;
        gridBagConstraints.ipady = 310;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        getContentPane().add(CategoryList, gridBagConstraints);

        jLabel3.setText("Categories:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        getContentPane().add(jLabel3, gridBagConstraints);

        jPanel1.setMinimumSize(new java.awt.Dimension(100, 100));
        jPanel1.setPreferredSize(new java.awt.Dimension(100, 100));
        jPanel1.setLayout(new java.awt.GridBagLayout());

        jLabel4.setText("Training Percentage (%)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        jPanel1.add(jLabel4, gridBagConstraints);

        MainTestBtn.setText("Execute Main Test");
        MainTestBtn.setComponentPopupMenu(OptionsMnu);
        MainTestBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MainTestBtnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        jPanel1.add(MainTestBtn, gridBagConstraints);

        jLabel5.setText("Pruning Factor (-5 to 5):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        jPanel1.add(jLabel5, gridBagConstraints);

        PruningFactorEd.setText("-2.5");
        PruningFactorEd.setMinimumSize(new java.awt.Dimension(26, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        jPanel1.add(PruningFactorEd, gridBagConstraints);

        TrainingPercentEd.setText("50");
        TrainingPercentEd.setMinimumSize(new java.awt.Dimension(26, 20));
        TrainingPercentEd.setPreferredSize(new java.awt.Dimension(26, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        jPanel1.add(TrainingPercentEd, gridBagConstraints);

        AnalyzeFileBtn.setText("Analyze File");
        AnalyzeFileBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                AnalyzeFileBtnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        jPanel1.add(AnalyzeFileBtn, gridBagConstraints);

        ClearCategoriesBtn.setForeground(new java.awt.Color(255, 51, 51));
        ClearCategoriesBtn.setText("Clear Categories");
        ClearCategoriesBtn.setMinimumSize(null);
        ClearCategoriesBtn.setPreferredSize(new java.awt.Dimension(91, 23));
        ClearCategoriesBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ClearCategoriesBtnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        jPanel1.add(ClearCategoriesBtn, gridBagConstraints);

        jLabel6.setText("Repetitions (Folds):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        jPanel1.add(jLabel6, gridBagConstraints);

        FoldsEd.setText("3");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        jPanel1.add(FoldsEd, gridBagConstraints);

        jLabel7.setText("Part of corpus (%)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        jPanel1.add(jLabel7, gridBagConstraints);

        PartOfCorpusEd.setText("50");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        jPanel1.add(PartOfCorpusEd, gridBagConstraints);

        SuggesterOnChk.setText("Suggester On");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        jPanel1.add(SuggesterOnChk, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.ipadx = 100;
        gridBagConstraints.ipady = 100;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        getContentPane().add(jPanel1, gridBagConstraints);

        StatusBar.setText("Status OK");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        getContentPane().add(StatusBar, gridBagConstraints);

        SecondaryStatusBar.setText("(Idle)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 13;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        getContentPane().add(SecondaryStatusBar, gridBagConstraints);

        FoldCountStatusBar.setText("Fold Count");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        getContentPane().add(FoldCountStatusBar, gridBagConstraints);

        jLabel8.setText("Training Set Dir:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        getContentPane().add(jLabel8, gridBagConstraints);

        TrainingSetDirEd.setEnabled(false);
        TrainingSetDirEd.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                TrainingSetDirEdMouseClicked(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        getContentPane().add(TrainingSetDirEd, gridBagConstraints);

        UseTrainingDirChk.setText("Use Training Dir");
        UseTrainingDirChk.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                UseTrainingDirChkItemStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        getContentPane().add(UseTrainingDirChk, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents

    private void StopProcessFoldMnuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_StopProcessFoldMnuActionPerformed
        bStopFold = true;
    }//GEN-LAST:event_StopProcessFoldMnuActionPerformed

    private void ShowSecondaryToolbarChkItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_ShowSecondaryToolbarChkItemStateChanged
        SecondaryStatusBar.setVisible(evt.getStateChange() == ItemEvent.SELECTED);
    }//GEN-LAST:event_ShowSecondaryToolbarChkItemStateChanged

    private void UseTrainingDirChkItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_UseTrainingDirChkItemStateChanged
        TrainingSetDirEd.setEnabled(evt.getStateChange() == ItemEvent.SELECTED);
        TrainingPercentEd.setEnabled(!(evt.getStateChange() == ItemEvent.SELECTED));
    }//GEN-LAST:event_UseTrainingDirChkItemStateChanged

    private void TrainingSetDirEdMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_TrainingSetDirEdMouseClicked
        // Select a dir
        JFileChooser fc = new JFileChooser();
        fc.setCurrentDirectory((DataDirEd.getText().length() == 0) ? new java.io.File(".") : new java.io.File(DataDirEd.getText()));
        fc.setSelectedFile(fc.getCurrentDirectory());
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int iRet = fc.showOpenDialog(this);
        if (iRet == JFileChooser.APPROVE_OPTION)
            TrainingSetDirEd.setText(fc.getSelectedFile().getAbsolutePath());
    }//GEN-LAST:event_TrainingSetDirEdMouseClicked
    
    private void ClearCategoriesBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ClearCategoriesBtnActionPerformed
        if (JOptionPane.showConfirmDialog(null, "Να διαγραφούν") == JOptionPane.YES_OPTION) {
            CategoryDecider.reset();
            UpdateCategories();
        }        
    }//GEN-LAST:event_ClearCategoriesBtnActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        // Save data to disk
        if (JOptionPane.showConfirmDialog(null, "Να αποθηκευτούν οι κατηγορίες;" ,
                "Παρακαλώ επιλέξτε", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
            return;
        INSECTFileDB fdData= new INSECTFileDB("", DataDirEd.getText());
        Iterator iIter = Arrays.asList(DataRepository.getObjectList(INSECTMemoryDB.CATEGORY_TYPE)).iterator();
        while (iIter.hasNext()) {
            String sObjectName = (String)iIter.next();
            fdData.saveObject((NamedDocumentNGramGraph)DataRepository.loadObject(sObjectName, INSECTMemoryDB.CATEGORY_TYPE),
                    sObjectName, INSECTMemoryDB.CATEGORY_TYPE);
        }
    }//GEN-LAST:event_formWindowClosing

    private void CorpusDirEdMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_CorpusDirEdMouseClicked
        CorpusDirEdActionPerformed(new java.awt.event.ActionEvent(evt.getComponent(), MouseEvent.BUTTON1, ""));
    }//GEN-LAST:event_CorpusDirEdMouseClicked

    private void DataDirEdActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DataDirEdActionPerformed
        // Select a dir
        JFileChooser fc = new JFileChooser();
        fc.setCurrentDirectory((DataDirEd.getText().length() == 0) ? new java.io.File(".") : new java.io.File(DataDirEd.getText()));
        fc.setSelectedFile(fc.getCurrentDirectory());
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int iRet = fc.showOpenDialog(this);
        if (iRet == JFileChooser.APPROVE_OPTION)
        {
            DataDirEd.setText(fc.getSelectedFile().getAbsolutePath());
            
            // Get categories from disk
            INSECTFileDB fdData= new INSECTFileDB("", DataDirEd.getText());
            Iterator iIter = Arrays.asList(fdData.getObjectList(INSECTMemoryDB.CATEGORY_TYPE)).iterator();
            while (iIter.hasNext()) {
                String sObjectName = (String)iIter.next();
                DataRepository.saveObject((NamedDocumentNGramGraph)fdData.loadObject(sObjectName,
                        INSECTMemoryDB.CATEGORY_TYPE), sObjectName, INSECTMemoryDB.CATEGORY_TYPE);
            }
            // Inform Categorizer
            CategoryDecider.setRepository(DataRepository);
            
            UpdateCategoriesByUIValues();
        }
    }//GEN-LAST:event_DataDirEdActionPerformed

    private void MainTestBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MainTestBtnActionPerformed
        Thread t = new Thread() {
            private MainINSECTForm parentForm;
            @Override
            public void run() {
                doMainTest();
            }
        };
        t.start();
    }//GEN-LAST:event_MainTestBtnActionPerformed

    private void doMainTest() {
            double dOverallPerformance = 0.0;
            int iAllDocs = 0;
            int iOverallSuccess = 0;
            long lOverallMillis = 0;

            for (int iFoldCnt = 0; iFoldCnt < Integer.valueOf(FoldsEd.getText()); iFoldCnt++) {
                // Update fold count
                FoldCountStatusBar.setText("Fold no." + String.valueOf(iFoldCnt+1));
                System.out.println("===Fold no." + String.valueOf(iFoldCnt+1));
                this.update(getGraphics());

                // Reset categories
                CategoryDecider.reset();
                // Create a document set
                UpdateStatus("Creating datasets");
                // If training directory selected then
                DocumentSet dsTraining;
                DocumentSet dsTesting;
                if (!TrainingSetDirEd.isEnabled())
                {
                    DocumentSet dsSet = new DocumentSet(CorpusDirEd.getText(), Double.valueOf(TrainingPercentEd.getText()) / 100);

                    // dsSet.createSets();
                    dsSet.createSets(true, Double.valueOf(PartOfCorpusEd.getText()) / 100); // Only use 1/10 of the corpus

                    UpdateStatus("Shuffling datasets");
                    dsSet.shuffleTrainingSet();
                    dsSet.shuffleTestSet();
                    dsTraining = dsSet;
                    dsTesting = dsSet;
                }
                else
                {
                    dsTraining = new DocumentSet(TrainingSetDirEd.getText(), 1.0);
                    dsTesting = new DocumentSet(CorpusDirEd.getText(), 0.0);
                    // dsSet.createSets();
                    //dsTraining.createSets(true, 1.0); // Full training
                    dsTraining.createSets(true, Double.valueOf(PartOfCorpusEd.getText()) / 100); // Only use 1/10 of the training
                    dsTesting.createSets(true, Double.valueOf(PartOfCorpusEd.getText()) / 100); // Only use 1/10 of the corpus
                    UpdateStatus("Shuffling datasets");
                    dsTraining.shuffleTrainingSet();
                    dsTesting.shuffleTestSet();
                }

                Date dStart = new Date();
                UpdateStatus("Train start");
                System.out.println("==Train start");
                CategorySuggester.clear();  // Reset suggester
                // Train
                Iterator iTrainIter = dsTraining.getTrainingSet().iterator();
                int iTrainingSize = dsTraining.getTrainingSet().size();
                int iCur = 0;
                while (iTrainIter.hasNext())
                {
                    CategorizedFileEntry feFile = (CategorizedFileEntry)iTrainIter.next();
                    UpdateSecondaryStatus("Processing file " + feFile.getFileName() + " of size " +
                            String.valueOf((new File(feFile.getFileName())).length()), 0.0);
                    checkFile(feFile.getFileName(), feFile.getCategory(), true);
                    Date dCurTime = new Date();            
                    long lRemaining = (iTrainingSize - iCur) * (long)((double)(dCurTime.getTime() - dStart.getTime()) / (++iCur)) *
                            (iTrainingSize / iCur);

                    UpdateStatus(String.format("Training in progress... (Remaining %s)", gr.demokritos.iit.jinsect.utils.millisToMinSecString(lRemaining)),
                            (double)iCur / iTrainingSize);

                }

                // Test in parallel
                System.out.println("==Test start");
                final Distribution<String> dSuccess = new Distribution<String>();
                final Distribution<String> dFailure = new Distribution<String>();
                final Distribution<String> dAll = new Distribution<String>();
                
                ThreadQueue tqThreads = new ThreadQueue();

                dStart = new Date();
                Iterator iTestIter = dsTesting.getTestSet().iterator();
                int iAll = dsTesting.getTestSet().size();
                int iSuccess = 0;
                iCur = 0;
                while (iTestIter.hasNext())
                {
                    final CategorizedFileEntry feFile = (CategorizedFileEntry)iTestIter.next();
                    dAll.increaseValue(feFile.getCategory(), 1.0);

                    while (!tqThreads.addThreadFor(new Runnable() {
                        @Override
                        public void run() {
                            if (checkFile(feFile.getFileName(),
                                    feFile.getCategory(), false))
                                synchronized (dSuccess) {
                                    dSuccess.increaseValue(feFile.getCategory(), 1.0);
                                }
                            else
                                synchronized (dFailure) {
                                    dFailure.increaseValue(feFile.getCategory(), 1.0);
                                }
                        }
                    }))
                        Thread.yield();
                    
                    Date dCurTime = new Date();
                    long lRemaining = (iAll - iCur) *
                        (long)((double)(dCurTime.getTime() - dStart.getTime())
                        / (++iCur));

                    synchronized (dSuccess) {
                        iSuccess = (int)dSuccess.sumOfValues();
                        UpdateStatus(String.format("Testing in progress... " +
                                "(Remaining %s) Success so far %3.2f",
                            gr.demokritos.iit.jinsect.utils.millisToMinSecString(lRemaining), 
                             (double)iSuccess / iCur),
                            (double)iCur / iAll);
                    }
                }
                
                try {
                    tqThreads.waitUntilCompletion();
                } catch (InterruptedException ex) {
                    Logger.getLogger(MainINSECTForm.class.getName()).log(Level.SEVERE, null, ex);
                }
                iSuccess = (int)dSuccess.sumOfValues();

                Date dEnd = new Date();
                long lMillis = dEnd.getTime() - dStart.getTime();

                // DEBUG LINES
//                System.out.println("Success of " + String.valueOf(iSuccess) + " out of " + String.valueOf(iAll) +
//                    ". Which is a " + String.format("%3.2f%% success. Elapsed time: %s",
//                    ((double)iSuccess / iAll) * 100,
//                    gr.demokritos.iit.jinsect.utils.millisToMinSecString(lMillis)));

                // Update overall            
                lOverallMillis += lMillis;
                iOverallSuccess += iSuccess;
                iAllDocs += iAll;
                dOverallPerformance += ((double)iSuccess / iAll);   

                // Stop signalled
                if (bStopFold) {
                    bStopFold = false;
                    break;
                }

                // Output overall performance details
                System.err.println(String.format("Fold #%d results",
                        iFoldCnt));
                System.err.println("Successes:\n\t" + dSuccess.toString());
                System.err.println("Failures:\n\t" + dFailure.toString());
                System.err.println("All counts:\n\t" + dAll.toString());

            }
            // Get the average performance
            dOverallPerformance /= Double.valueOf(FoldsEd.getText());

            JOptionPane.showMessageDialog(null, "Success of " + String.valueOf(iOverallSuccess) + " out of " + 
                    String.valueOf(iAllDocs) + ". Which is a " + 
                    String.format("%3.2f%% success. Elapsed time: %s",
                    (double)iOverallSuccess / iAllDocs * 100,
                    gr.demokritos.iit.jinsect.utils.millisToMinSecString(lOverallMillis)));        
    }
    private void CorpusDirEdActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CorpusDirEdActionPerformed
        // Select a dir
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setCurrentDirectory((CorpusDirEd.getText().length() == 0) ? new java.io.File(".") : new java.io.File(CorpusDirEd.getText()));
        fc.setSelectedFile(fc.getCurrentDirectory());
        int iRet = fc.showOpenDialog(this);
        if (iRet == JFileChooser.APPROVE_OPTION)
            CorpusDirEd.setText(fc.getSelectedFile().getAbsolutePath());
    }//GEN-LAST:event_CorpusDirEdActionPerformed

    private void DataDirEdMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_DataDirEdMouseClicked
        DataDirEdActionPerformed(new java.awt.event.ActionEvent(evt.getComponent(), evt.getButton(), ""));
    }//GEN-LAST:event_DataDirEdMouseClicked

    private void AnalyzeFileBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_AnalyzeFileBtnActionPerformed
        // Select a file
        JFileChooser fc = new JFileChooser();
        fc.setCurrentDirectory(new java.io.File("."));
        int iRes = fc.showOpenDialog(this);
        
        if (iRes != JFileChooser.APPROVE_OPTION)
            return;
        
        checkFile(fc.getSelectedFile().getAbsolutePath(), NEW_CATEGORY, true);
    }//GEN-LAST:event_AnalyzeFileBtnActionPerformed
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton AnalyzeFileBtn;
    private javax.swing.JList CategoryList;
    private javax.swing.JButton ClearCategoriesBtn;
    private javax.swing.JTextField CorpusDirEd;
    private javax.swing.JTextField DataDirEd;
    private javax.swing.JLabel FoldCountStatusBar;
    private javax.swing.JTextField FoldsEd;
    private javax.swing.JButton MainTestBtn;
    private javax.swing.JPopupMenu OptionsMnu;
    private javax.swing.JTextField PartOfCorpusEd;
    private javax.swing.JTextField PruningFactorEd;
    private javax.swing.JLabel SecondaryStatusBar;
    private javax.swing.JCheckBoxMenuItem ShowSecondaryToolbarChk;
    private javax.swing.JLabel StatusBar;
    private javax.swing.JMenuItem StopProcessFoldMnu;
    private javax.swing.JCheckBox SuggesterOnChk;
    private javax.swing.JTextField TrainingPercentEd;
    private javax.swing.JTextField TrainingSetDirEd;
    private javax.swing.JCheckBox UseTrainingDirChk;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JPanel jPanel1;
    // End of variables declaration//GEN-END:variables
    
}
