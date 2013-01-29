/*
 * @(#)Keys.java - static, fixed keys
 *
 * Copyright (c) 2005-2013 by dvb.matt, All rights reserved.
 * 
 * This file is part of ProjectX, a free Java based demux utility.
 * By the authors, ProjectX is intended for educational purposes only, 
 * as a non-commercial test project.
 * 
 *
 * This program is free software; you can redistribute it and/or modify 
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */
package net.sourceforge.dvb.projectx.common;

import net.sourceforge.dvb.projectx.common.Resource;


public class Keys extends Object {

	public final static String KEY_Tip = ".Tip";

	// General
	public final static String[] KEY_Agreement = { "Application.Agreement", "0" }; //rbutton[1]
	public final static String[] KEY_ActiveDirectory = { "Application.ActiveDirectory", "" }; 
	public final static String[] KEY_LookAndFeel = { "Application.LookAndFeel", "" };
	public final static String[] KEY_InputDirectoriesDepth = { "Application.InputDirectoriesDepth", "0" };
	public final static String KEY_InputDirectories = "Application.InputDirectories.";
	public final static String KEY_OutputDirectories = "Application.OutputDirectories.";
	public final static String[] KEY_OutputDirectory = { "Application.OutputDirectory", "" };
	public final static String[] KEY_Language = { "Application.Language", "en" };
	public final static String[] KEY_SaveSettingsOnExit = { "Application.SaveSettingsOnExit", "1" };

	public final static String[] KEY_FtpServer_Commands = { "FtpServer.Commands", "" }; 
	public final static String[] KEY_WindowPositionMain_X = { "WindowPosition.Main.X", "50" }; 
	public final static String[] KEY_WindowPositionMain_Y = { "WindowPosition.Main.Y", "50" }; 
	public final static String[] KEY_WindowPositionMain_Width = { "WindowPosition.Main.Width", "852" }; //866
	public final static String[] KEY_WindowPositionMain_Height = { "WindowPosition.Main.Height", "632" }; //626

	// PostCommands
	public final static String[] KEY_PostCommands_Cmd1 = { "PostCommands.Cmd1", "" }; 
	public final static String[] KEY_PostCommands_Cmd2 = { "PostCommands.Cmd2", "" }; 
	public final static String[] KEY_PostCommands_Cmd3 = { "PostCommands.Cmd3", "" }; 
	public final static String[] KEY_PostCommands_Cmd4 = { "PostCommands.Cmd4", "" }; 
	public final static String[] KEY_PostCommands_Cmd5 = { "PostCommands.Cmd5", "" }; 
	public final static String[] KEY_PostCommands_Cmd6 = { "PostCommands.Cmd6", "" }; 
	public final static String[] KEY_PostCommands_Cmd7 = { "PostCommands.Cmd7", "" }; 
	public final static String[] KEY_PostCommands_Cmd8 = { "PostCommands.Cmd8", "" }; 

	// MessagePanel
	public final static String[] KEY_MessagePanel_Msg1 = { "MessagePanel.logSequenceError", "1" }; //cbox[40] sequence error
	public final static String[] KEY_MessagePanel_Msg2 = { "MessagePanel.logMissingStartcode", "1" }; //cbox[3]  missing startcode
	public final static String[] KEY_MessagePanel_Msg3 = { "MessagePanel.logESError", "1" }; //cbox[74] pes in es 
	public final static String[] KEY_MessagePanel_Msg4 = { "MessagePanel.leadingTimeIndex", "0" }; //cbox[72] timeindex
	public final static String[] KEY_MessagePanel_Msg5 = { "MessagePanel.logWSS", "1" }; //WSS
	public final static String[] KEY_MessagePanel_Msg6 = { "MessagePanel.logVPS", "1" }; //VPS
	public final static String[] KEY_MessagePanel_Msg7 = { "MessagePanel.logRDS", "1" }; //RDS
	public final static String[] KEY_MessagePanel_Msg8 = { "MessagePanel.logErrorMaximum", "1" };

	// MainPanel
	public static Object[] ITEMS_ConversionMode = null;

	public final static String[] KEY_ConversionMode = { "MainPanel.ConversionMode", "0" }; //combox[19], index
	public final static String[] KEY_ConversionModePriority = { "MainPanel.ConversionModePriority", "0" };
	public final static String[] KEY_useAllCollections = { "MainPanel.useAllCollections", "0" }; //cbox[18]
	public final static String[] KEY_enablePostProcessing = { "MainPanel.enablePostProcessing", "0" }; //cbox[25]
	public final static String[] KEY_minimizeMainFrame = { "MessagePanel.minimizeMainFrame", "0" }; 
	public final static String[] KEY_hideProcessWindow = { "MessagePanel.hideProcessWindow", "0" }; 
	public final static String[] KEY_showSubpictureWindow = { "MessagePanel.showSubpictureWindow", "0" }; 
	public final static String[] KEY_simpleMPG = { "MainPanel.simpleMPG", "0" }; //cbox[14]
	public final static String[] KEY_enhancedPES = { "MainPanel.enhancedPES", "0" }; //cbox[14]
	public final static String[] KEY_useAutoPidFilter = { "MainPanel.useAutoPidFilter", "0" };
	public final static String[] KEY_PostProcessCompletion = { "MainPanel.PostProcessCompletion", "0" };
	public final static String[] KEY_useGOPEditor = { "MainPanel.useGOPEditor", "0" };

	// LogWindowPanel
	public final static String[] KEY_showTtxHeader = { "LogwindowPanel.showTtxHeader", "0" }; //cbox[19]


	// ExportPanel
	public final static String[] KEY_SplitSize = { "ExportPanel.SplitSize", "0" }; //cbox[5]
	public final static String[] KEY_Streamtype_MpgVideo = { "ExportPanel.Streamtype.MpgVideo", "1" }; //cbox[55]
	public final static String[] KEY_Streamtype_MpgAudio = { "ExportPanel.Streamtype.MpgAudio", "1" }; //cbox[56]
	public final static String[] KEY_Streamtype_Ac3Audio = { "ExportPanel.Streamtype.Ac3Audio", "1" }; //cbox[57]
	public final static String[] KEY_Streamtype_PcmAudio = { "ExportPanel.Streamtype.PcmAudio", "1" }; //cbox[58]
	public final static String[] KEY_Streamtype_Teletext = { "ExportPanel.Streamtype.Teletext", "1" }; //cbox[59]
	public final static String[] KEY_Streamtype_Subpicture = { "ExportPanel.Streamtype.Subpicture", "1" }; //cbox[60]
	public final static String[] KEY_Streamtype_Vbi = { "ExportPanel.Streamtype.Vbi", "1" }; //cbox[81]
	public final static String[] KEY_WriteOptions_writeVideo = { "ExportPanel.WriteOptions.writeVideo", "1" }; //cbox[6]
	public final static String[] KEY_WriteOptions_writeAudio = { "ExportPanel.WriteOptions.writeAudio", "1" }; //cbox[7]
	public final static String[] KEY_additionalOffset = { "ExportPanel.additionalOffset", "0" }; //cbox[8]
	public final static String[] KEY_ExportPanel_Export_Overlap = { "ExportPanel.Overlap", "0" }; 
	public final static String[] KEY_ExportPanel_createSubDirNumber = { "ExportPanel.createSubDirNumber", "0" }; //cbox[2]
	public final static String[] KEY_ExportPanel_createSubDirName = { "ExportPanel.createSubDirName", "0" }; //cbox[71]
	public final static String[] KEY_ExportPanel_createSubDirVdr =  { "ExportPanel.createSubDirVdr", "0" };
	public final static String[] KEY_ExportPanel_SplitSize_Value = { "ExportPanel.SplitSize.Value", "650" }; //combox[2]
	public final static String[] KEY_ExportPanel_Overlap_Value = { "ExportPanel.Overlap.Value", "0" }; //combox[25]
	public final static String[] KEY_ExportPanel_Infoscan_Value = { "ExportPanel.Infoscan.Value", "5" }; //combox[21]
	public final static String[] KEY_ExportPanel_additionalOffset_Value = { "ExportPanel.additionalOffset.Value", "0" }; //combobox[8], item
	public final static String[] KEY_LanguageFilter = { "ExportPanel.Language.Filter", "" };

	public static Object[] ITEMS_Export_SplitSize = {
		"650", "700", "735", "792", "2000", "4700"
	};

	public static Object[] ITEMS_Export_Overlap = {
		"1 MB", "2 MB", "3 MB", "4 MB", "5 MB", "6 MB", "7 MB", "8 MB", "9 MB", "10 MB"
	};

	public static Object[] ITEMS_Infoscan = { "5", "10", "25" };


	// OptionPanel
	public final static String[] KEY_DebugLog = { "OptionPanel.DebugLog", "0" }; //cbox[11]  - group1
	public final static String[] KEY_NormalLog = { "OptionPanel.NormalLog", "1" }; //cbox[21] -group1
	public final static String[] KEY_dumpDroppedGop = { "OptionPanel.dumpDroppedGop", "0" }; //cbox[43] 
	public final static String[] KEY_closeOnEnd = { "OptionPanel.closeOnEnd", "0" }; //cbox[78] 
	public final static String[] KEY_StartPath_Value = { "OptionPanel.StartPath.Value", "" }; 
	public final static String[] KEY_MainBuffer = { "OptionPanel.MainBuffer", "4096000" }; //combobox[10]
	public final static String[] KEY_ScanBuffer = { "OptionPanel.ScanBuffer", "1024000" }; //combobox[37]
	public final static String[] KEY_PreviewBuffer = { "OptionPanel.PreviewBuffer", "1024000" }; //combobox[38]
	public final static String[] KEY_holdStreamInfoOnOSD = { "OptionPanel.holdStreamInfoOnOSD", "0" };
	public final static String[] KEY_OptionPanelIndex = { "OptionPanel.PanelIndex", "0" };
	public final static String[] KEY_additionalInputBuffer = { "OptionPanel.additionalInputBuffer", "0" }; //test
	public final static String[] KEY_enableHDDemux = { "OptionPanel.enableHDDemux", "0" };

	// SpecialPanel
	public final static String[] KEY_PVA_FileOverlap = { "SpecialPanel.PVA.FileOverlap", "0" }; //cbox[48]
	public final static String[] KEY_PVA_Audio = { "SpecialPanel.PVA.Audio", "1" }; //cbox[28], true
	public final static String[] KEY_VOB_resetPts = { "SpecialPanel.VOB.resetPts", "1" }; //cbox[76], true
	public final static String[] KEY_TS_ignoreScrambled = { "SpecialPanel.TS.ignoreScrambled", "1" }; //cbox[38], true
	public final static String[] KEY_TS_blindSearch = { "SpecialPanel.TS.blindSearch", "1" }; //cbox[61], true
	public final static String[] KEY_TS_joinPackets = { "SpecialPanel.TS.joinPackets", "1" }; //cbox[53], true
	public final static String[] KEY_TS_HumaxAdaption = { "SpecialPanel.TS.HumaxAdaption", "0" }; //cbox[70]
	public final static String[] KEY_TS_FinepassAdaption = { "SpecialPanel.TS.FinepassAdaption", "0" };
	public final static String[] KEY_TS_JepssenAdaption = { "SpecialPanel.TS.JepssenAdaption", "0" };
	public final static String[] KEY_TS_KoscomAdaption = { "SpecialPanel.TS.KoscomAdaption", "0" };
	public final static String[] KEY_TS_ArionAdaption = { "SpecialPanel.TS.ArionAdaption", "0" };
	public final static String[] KEY_TS_generatePmt = { "SpecialPanel.TS.generatePmt", "1" }; //cbox[41], true
	public final static String[] KEY_TS_generateTtx = { "SpecialPanel.TS.generateTtx", "0" }; //cbox[42] --ts !!
	public final static String[] KEY_TS_setMainAudioAc3 = { "SpecialPanel.TS.setMainAudioAc3", "0" }; //cbox[37] --ts  !!
	public final static String[] KEY_Input_getEnclosedPackets = { "SpecialPanel.Input.getEnclosedPackets", "1" }; //cbox[33], true
	public final static String[] KEY_Input_concatenateForeignRecords = { "SpecialPanel.Input.concatenateForeignRecords", "1" }; //cbox[49], true
	public final static String[] KEY_Input_useReadOverhead = { "SpecialPanel.Input.useReadOverhead", "1" };
	public final static String[] KEY_Audio_ignoreErrors = { "SpecialPanel.Audio.ignoreErrors", "0" }; //cbox[24] 
	public final static String[] KEY_Audio_limitPts = { "SpecialPanel.Audio.limitPts", "0" }; //cbox[15]
	public final static String[] KEY_Audio_allowFormatChanges = { "SpecialPanel.Audio.allowFormatChanges", "0" };
	public final static String[] KEY_Video_ignoreErrors = { "SpecialPanel.Video.ignoreErrors", "0" }; //cbox[39]
	public final static String[] KEY_Video_trimPts = { "SpecialPanel.Video.trimPts", "0" }; //cbox[73]
	public final static String[] KEY_Video_cutPts = { "SpecialPanel.Video.cutPts", "0" }; 
	public final static String[] KEY_Conversion_startWithVideo = { "SpecialPanel.Conversion.startWithVideo", "1" }; //cbox[23] -streamconv !!
	public final static String[] KEY_Conversion_addPcrToStream = { "SpecialPanel.Conversion.addPcrToStream", "1" }; //cbox[36] -streamconv !!
	public final static String[] KEY_Conversion_PcrCounter = { "SpecialPanel.Conversion.PcrCounter", "0" }; //cbox[46] -streamconv !!

	public final static String[] KEY_TsHeaderMode = { "SpecialPanel.TS.HeaderMode", "0" }; //combox[20]
	public final static String[] KEY_PtsShift_Value = { "SpecialPanel.PtsShift.Value", "0" }; //combox[27]
	public final static String[] KEY_PcrDelta_Value = { "SpecialPanel.PcrDelta.Value", "65000" }; //combox[23]

	public static Object[] ITEMS_TsHeaderMode = null;

	public static Object[] ITEMS_PtsShift = { 
		"auto", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13"
	};

	public static Object[] ITEMS_PcrDelta = { 
		"25000", "35000", "45000", "55000", "65000", "80000", "100000", "125000", "150000" 
	};


	// ExternPanel
	public final static String[] KEY_ExternPanel_createVdrIndex = { "ExternPanel.createVdrIndex", "0" }; //cbox[54]
	public final static String[] KEY_ExternPanel_createCellTimes = { "ExternPanel.createCellTimes", "1" }; //cbox[26] !!
	public final static String[] KEY_ExternPanel_exportPts = { "ExternPanel.exportPts", "0" }; //cbox[64] !!
	public final static String[] KEY_ExternPanel_save1stFrameOfGop = { "ExternPanel.save1stFrameOfGop", "0" }; //cbox[65]
	public final static String[] KEY_ExternPanel_createChapters = { "ExternPanel.createChapters", "0" }; //cbox[63]
	public final static String[] KEY_ExternPanel_renameAudio = { "ExternPanel.renameAudio", "0" }; //cbox[16]
	public final static String[] KEY_ExternPanel_renameVideo = { "ExternPanel.renameVideo", "0" }; //cbox[32]
	public final static String[] KEY_ExternPanel_appendExtension = { "ExternPanel.appendExtension", "0" }; //cbox[66] !!
	public final static String[] KEY_ExternPanel_createM2sIndex = { "ExternPanel.createM2sIndex", "0" }; //cbox[34] 
	public final static String[] KEY_ExternPanel_createD2vIndex = { "ExternPanel.createD2vIndex", "0" }; //cbox[29] 
	public final static String[] KEY_ExternPanel_createDgiIndex = { "ExternPanel.createDgiIndex", "0" }; //cbox[82] !!
	public final static String[] KEY_ExternPanel_D2VOptions = { "ExternPanel.D2VOptions", "iDCT_Algorithm=2|YUVRGB_Scale=1|Luminance=128,0|Picture_Size=0,0,0,0,0,0|Field_Operation=0" };
	public final static String[] KEY_ExternPanel_splitProjectFile = { "ExternPanel.splitProjectFile", "0" }; //cbox[30] !!
	public final static String[] KEY_ExternPanel_ProjectFileSplitSize = { "ExternPanel.ProjectFileSplitSize", "2048" }; 
    public final static String[] KEY_ExternPanel_createInfoIndex = { "ExternPanel.createInfoIndex", "0" };
	public final static String[] KEY_ExternPanel_appendPidToFileName = { "ExternPanel.appendPidToFileName", "0" };
	public final static String[] KEY_ExternPanel_appendLangToFileName = { "ExternPanel.appendLangToFileName", "0" };

	public final static String[] KEY_killFtpClient = { "FtpPanel.killFtpClient", "0" }; //cbox[80] !!
	public final static String[] KEY_useFtpServerResume = { "FtpPanel.useFtpServerResume", "1" };
	public final static String[] KEY_autostartWebServer = { "NetPanel.autostartWebServer", "0" };
	public final static String[] KEY_WebServerPort = { "NetPanel.WebServerPort", "-1" };
	public final static String[] KEY_WebServerAccess = { "NetPanel.WebServerAccess", "" };


	// VideoPanel
	public final static String[] KEY_VideoPanel_addEndcode = { "VideoPanel.addEndcode", "1" }; //cbox[13] 
	public final static String[] KEY_VideoPanel_insertEndcode = { "VideoPanel.insertEndcode", "0" }; //cbox[75] 
	public final static String[] KEY_VideoPanel_addSequenceHeader = { "VideoPanel.addSequenceHeader", "0" }; //cbox[27] 
	public final static String[] KEY_VideoPanel_clearCDF = { "VideoPanel.clearCdf", "1" }; //cbox[35] 
	public final static String[] KEY_VideoPanel_patchToProgressive = { "VideoPanel.patchToProgressive", "0" }; //cbox[31] 
	public final static String[] KEY_VideoPanel_patchToInterlaced = { "VideoPanel.patchToInterlaced", "0" }; //cbox[44] 
	public final static String[] KEY_VideoPanel_toggleFieldorder = { "VideoPanel.toggleFieldorder", "0" }; //cbox[45] 
	public final static String[] KEY_VideoPanel_addSde = { "VideoPanel.addSde", "1" }; //cbox[77] 
	public final static String[] KEY_VideoPanel_SdeValue = { "VideoPanel.SdeValue", "" }; 

	public final static String[] KEY_ChangeVbvBuffer = { "VideoPanel.ChangeVbvBuffer", "0" };  //combox[4]
	public final static String[] KEY_ChangeVbvDelay = { "VideoPanel.ChangeVbvDelay", "1" };  //combox[5]
	public final static String[] KEY_ChangeAspectRatio = { "VideoPanel.ChangeAspectRatio", "0" }; //combox[6]
	public final static String[] KEY_ChangeBitrateInAllSequences = { "VideoPanel.ChangeBitrateInAllSequences", "1" };  //combox[3]
	public final static String[] KEY_ChangeBitrateInFirstSequence = { "VideoPanel.ChangeBitrateInFirstSequence", "2" }; //combox[15]

	public final static String[] KEY_ConditionalHorizontalPatch = { "VideoPanel.ConditionalHorizontalPatch", "0" }; //combox[35]
	public final static String[] KEY_ConditionalHorizontalResolution = { "VideoPanel.ConditionalHorizontalResolution", "352" }; //combox[22]

	public static Object[] ITEMS_ConditionalHorizontalPatch = null;
	public static Object[] ITEMS_ChangeVbvBuffer = null;
	public static Object[] ITEMS_ChangeVbvDelay = null;
	public static Object[] ITEMS_ChangeAspectRatio = null;
	public static Object[] ITEMS_BitrateInAllSequences = null;
	public static Object[] ITEMS_BitrateInFirstSequence = null;


	public final static String[] KEY_SubtitlePanel_decodeMegaradio = { "SubtitlePanel.decodeMegaradio", "0" }; //cbox[17] 
	public final static String[] KEY_SubtitlePanel_decodeHiddenRows = { "SubtitlePanel.decodeHiddenRows", "0" }; //cbox[22] 
	public final static String[] KEY_SubtitlePanel_rebuildPTStoggle = { "SubtitlePanel.rebuildPTStoggle", "0" };
	public final static String[] KEY_SubtitlePanel_rebuildPTS = { "SubtitlePanel.rebuildPTS", "0" }; //cbox[62] !!
	public final static String[] KEY_SubtitlePanel_rebuildPictPTS = { "SubtitlePanel.rebuildPictPTS", "0" };
	public final static String[] KEY_SubtitlePanel_keepOriginalTimecode = { "SubtitlePanel.keepOriginalTimecode", "0" }; //cbox[67] 
	public final static String[] KEY_SubtitlePanel_exportTextAsUnicode = { "SubtitlePanel.exportTextAsUnicode", "0" };
	public final static String[] KEY_SubtitlePanel_exportTextAsUTF8 = { "SubtitlePanel.exportTextAsUTF8", "0" };
	public final static String[] KEY_SubtitlePanel_useTextOutline = { "SubtitlePanel.useTextOutline", "1" }; //cbox[79] !!
	public final static String[] KEY_SubtitlePanel_Format_SUP_Values = { "SubtitlePanel.Format.SUP.Values", "26;10;32;80;560;720;576;-1;4;3;1" }; 
	public final static String[] KEY_SubtitlePanel_PageId_Value = { "SubtitlePanel.PageId.Value", "" }; 
	public final static String[] KEY_SubtitlePanel_MaxParityErrors = { "SubtitlePanel.maxParityErrors", "2" }; 

	public final static String[] KEY_SubtitlePanel_TtxPage1 = { "SubtitlePanel.TtxPage1", "null" }; //combobox[28]
	public final static String[] KEY_SubtitlePanel_TtxPage2 = { "SubtitlePanel.TtxPage2", "null" }; //combobox[29]
	public final static String[] KEY_SubtitlePanel_TtxPage3 = { "SubtitlePanel.TtxPage3", "null" }; //combobox[30]
	public final static String[] KEY_SubtitlePanel_TtxPage4 = { "SubtitlePanel.TtxPage4", "null" }; //combobox[31]
	public final static String[] KEY_SubtitlePanel_TtxPage5 = { "SubtitlePanel.TtxPage5", "null" }; //combobox[32]
	public final static String[] KEY_SubtitlePanel_TtxPage6 = { "SubtitlePanel.TtxPage6", "null" }; //combobox[33]
	public final static String[] KEY_SubtitlePanel_TtxPage7 = { "SubtitlePanel.TtxPage7", "null" }; //combobox[33]
	public final static String[] KEY_SubtitlePanel_TtxPage8 = { "SubtitlePanel.TtxPage8", "null" }; //combobox[33]
	public final static String[] KEY_TtxLanguagePair = { "SubtitlePanel.TtxLanguagePair", "0" }; //combobox[18], index
	public final static String[] KEY_SubtitleFont = { "SubtitlePanel.SubtitleFont", "Tahoma" }; //combobox[26], item
	public final static String[] KEY_SubtitleExportFormat = { "SubtitlePanel.SubtitleExportFormat", "SUB" };
	public final static String[] KEY_SubtitleExportFormat_2 = { "SubtitlePanel.SubtitleExportFormat_2", "null" };
	public final static String[] KEY_SubpictureColorModel = { "SubtitlePanel.SubpictureColorModel", "(0) 4 colors" };
	public final static String[] KEY_SubtitleChangeDisplay = { "SubtitlePanel.ChangeDisplay", "0" };
	public final static String[] KEY_SubtitleMovePosition_Value = { "SubtitlePanel.MovePosition.Value", "" };
	public final static String[] KEY_SubtitlePanel_specialTermination = { "SubtitlePanel.specialTermination", "1" };
	public final static String[] KEY_SubtitlePanel_keepColourTable = { "SubtitlePanel.keepColourTable", "0" };
	public final static String[] KEY_SubtitlePanel_exportAsVobSub = { "SubtitlePanel.exportAsVobSub", "0" };
	public final static String[] KEY_SubtitlePanel_TtxExportBoxedOnly = { "SubtitlePanel.TtxExportBoxedOnly", "0" };
	public final static String[] KEY_SubtitlePanel_useTextAlignment = { "SubtitlePanel.useTextAlignment", "0" };

	public final static String[] KEY_SubtitlePanel_enableHDSub = { "SubtitlePanel.enableHDSub", "0" };

	public static Object[] ITEMS_TtxLanguagePair = { 
		"auto", "basic latin", "polish", "turkish", "cro,slo,rum", "est,lit,rus",
		"res.",	"greek,latin", 	"res.", "arabic,latin", "res.", "hebrew,arabic"
	};

	public static Object[] ITEMS_SubtitleExportFormat = null;
	public static Object[] ITEMS_SubtitleChangeDisplay = null;


	public final static String[] KEY_AudioPanel_decodeMpgAudio = { "AudioPanel.decodeMpgAudio", "0" }; //cbox[50] 
	public final static String[] KEY_AudioPanel_validateCRC = { "AudioPanel.validateCRC", "1" }; //cbox[68] 
	public final static String[] KEY_AudioPanel_clearCRC = { "AudioPanel.clearCRC", "1" }; //cbox[1] 
	public final static String[] KEY_AudioPanel_fillGapsWithLastFrame = { "AudioPanel.fillGapsWithLastFrame", "0" }; //cbox[0] 
	public final static String[] KEY_AudioPanel_addFrames = { "AudioPanel.addFrames", "1" }; //cbox[20] 
	public final static String[] KEY_AudioPanel_AC3_patch1stHeader = { "AudioPanel.patch1stAc3Header", "0" }; //cbox[9] 
	public final static String[] KEY_AudioPanel_AC3_replaceWithSilence = { "AudioPanel.replaceAc3withSilence", "0" }; //cbox[10] 
	public final static String[] KEY_AudioPanel_AC3_BitrateAdaption = { "AudioPanel.AC3BitrateAdaption", "0" };
	public final static String[] KEY_AudioPanel_allowSpaces = { "AudioPanel.allowSpaces", "0" }; //cbox[69] 
	public final static String[] KEY_AudioPanel_addRiffToAc3 = { "AudioPanel.addRiffToAc3", "0" }; //cbox[12] 
	public final static String[] KEY_AudioPanel_addRiffToMpgAudio = { "AudioPanel.addRiffToMpgAudioL12", "0" }; //cbox[4] + rbutton[14] riff für layer1+2
	public final static String[] KEY_AudioPanel_pitchAudio = { "AudioPanel.pitchAudio", "0" }; //cbox[51] 
	public final static String[] KEY_AudioPanel_Normalize = { "AudioPanel.decodeMpgAudio.Normalize", "0" }; //rbutton[2] + exefield[8] normalize
	public final static String[] KEY_AudioPanel_Downmix = { "AudioPanel.decodeMpgAudio.Downmix", "0" }; //rbutton[3] 
	public final static String[] KEY_AudioPanel_changeByteorder = { "AudioPanel.decodeMpgAudio.changeByteorder", "0" }; //rbutton[4] 
	public final static String[] KEY_AudioPanel_addRiffHeader = { "AudioPanel.decodeMpgAudio.addRiffHeader", "1" }; //rbutton[5] 
	public final static String[] KEY_AudioPanel_addAiffHeader = { "AudioPanel.decodeMpgAudio.addAiffHeader", "0" }; //rbutton[9] 
	public final static String[] KEY_AudioPanel_addRiffToMpgAudioL3 = { "AudioPanel.addRiffToMpgAudioL3", "0" }; //cbox[4] + rbutton[15] riff für layer3
	public final static String[] KEY_AudioPanel_PitchValue = { "AudioPanel.PitchValue", "0" }; 
	public final static String[] KEY_AudioPanel_NormalizeValue = { "AudioPanel.NormalizeValue", "98" }; 
	public final static String[] KEY_AudioPanel_createDDWave = { "AudioPanel.createDDWave", "0" };
	public final static String[] KEY_AudioPanel_fadeInOut = { "AudioPanel.fadeInOut", "0" };
	public final static String[] KEY_AudioPanel_fadeInOutMillis = { "AudioPanel.fadeInOutMillis", "2000" };

	public final static String[] KEY_AudioPanel_losslessMpaConversionMode = { "AudioPanel.losslessMpaConversionMode", "0" }; 

	public static Object[] ITEMS_losslessMpaConversionMode = null;

	public final static String[] KEY_AudioPanel_resampleAudioMode = { "AudioPanel.decodeMpgAudio.resampleAudioMode", "0" }; 

	public static Object[] ITEMS_resampleAudioMode = null;

	public final static String[] KEY_Preview_disable = { "CollectionPanel.Preview.disable", "0" }; 
	public final static String[] KEY_Preview_fastDecode = { "CollectionPanel.Preview.fastDecode", "0" }; //rbutton[10] 
	public final static String[] KEY_Preview_LiveUpdate = { "CollectionPanel.Preview.LiveUpdate", "1" }; //rbutton[16] 
	public final static String[] KEY_Preview_fullScaled = { "CollectionPanel.Preview.fullScaled", "0" };
	public final static String[] KEY_Preview_AllGops = { "CollectionPanel.Preview.AllGops", "0" }; //rbutton[6] 
	public final static String[] KEY_Preview_SliderWidth = { "CollectionPanel.Preview.SliderWidth", "1" };
	public final static String[] KEY_Preview_YGain = { "CollectionPanel.Preview.YGain", "0" };
	public final static String[] KEY_OptionHorizontalResolution = { "CollectionPanel.OptionHorizontalResolution", "0" }; //cbox[52]
	public final static String[] KEY_OptionDAR = { "CollectionPanel.OptionDAR", "0" }; //cbox[47]

	public final static String[] KEY_ExportHorizontalResolution = { "CollectionPanel.ExportHorizontalResolution", "720" }; //combox[34]
	public final static String[] KEY_ExportDAR = { "CollectionPanel.ExportDAR", "2" }; //combox[24]
	public final static String[] KEY_CutMode = { "CollectionPanel.CutMode", "0" }; //combox[17]

	public static Object[] ITEMS_ExportHorizontalResolution = { 
		"304", "320", "352", "384", "480", "528", "544", "576", "640", "704", "720"
	};

	public static Object[] ITEMS_ExportDAR = {
		"1.000 (1:1)", "0.6735 (4:3)", "0.7031 (16:9)", "0.7615 (2.21:1)", "0.8055", 
		"0.8437", "0.9375", "0.9815", "1.0255", "1.0695", "1.1250", "1.1575", "1.2015"
	};

	public static Object[] ITEMS_CutMode = null;

	public static Object[] ITEMS_FileTypes = null;


	/**
	 * Constructor
	 */
	public Keys()
	{
		Object[] ITEMS_ConversionMode = { 
			Resource.getString("MainPanel.ConversionMode.demux"),
			Resource.getString("MainPanel.ConversionMode.toVDR"),
			Resource.getString("MainPanel.ConversionMode.toM2P"),
			Resource.getString("MainPanel.ConversionMode.toPVA"),
			Resource.getString("MainPanel.ConversionMode.toTS"),
			Resource.getString("MainPanel.ConversionMode.PidFilter"),
			Resource.getString("MainPanel.ConversionMode.binaryCopy")
		};
		this.ITEMS_ConversionMode = ITEMS_ConversionMode;

		Object[] ITEMS_TsHeaderMode = { 
			Resource.getString("SpecialPanel.TS.HeaderMode0"), 
			Resource.getString("SpecialPanel.TS.HeaderMode1"), 
			Resource.getString("SpecialPanel.TS.HeaderMode2"), 
			Resource.getString("SpecialPanel.TS.HeaderMode3"),
			Resource.getString("SpecialPanel.TS.HeaderMode4"), 
			Resource.getString("SpecialPanel.TS.HeaderMode5")
		};
		this.ITEMS_TsHeaderMode = ITEMS_TsHeaderMode;

		Object[] ITEMS_ConditionalHorizontalPatch = { 
			Resource.getString("VideoPanel.patchResolutionValue.0"),
			Resource.getString("VideoPanel.patchResolutionValue.1"),
			Resource.getString("VideoPanel.patchResolutionValue.2"),
			Resource.getString("VideoPanel.patchResolutionValue.3")
		};
		this.ITEMS_ConditionalHorizontalPatch = ITEMS_ConditionalHorizontalPatch;

		Object[] ITEMS_ChangeVbvBuffer = { 
			Resource.getString("VideoPanel.Unchanged"),
			Resource.getString("VideoPanel.ChangeVbvBuffer.Mode1")
		};
		this.ITEMS_ChangeVbvBuffer = ITEMS_ChangeVbvBuffer;

		Object[] ITEMS_ChangeVbvDelay = { 
			Resource.getString("VideoPanel.Unchanged"),
			Resource.getString("VideoPanel.ChangeVbvDelay.Mode1")
		};
		this.ITEMS_ChangeVbvDelay = ITEMS_ChangeVbvDelay;

		Object[] ITEMS_ChangeAspectRatio = { 
			Resource.getString("VideoPanel.Unchanged"),
			"1.000 (1:1)", "0.6735 (4:3)", "0.7031 (16:9)", "0.7615 (2.21:1)", "0.8055",
			"0.8437", "0.9375", "0.9815", "1.0255", "1.0695", "1.1250", "1.1575", "1.2015"
		};
		this.ITEMS_ChangeAspectRatio = ITEMS_ChangeAspectRatio;

		Object[] ITEMS_BitrateInAllSequences = {
			Resource.getString("VideoPanel.patchBitrateValue.0"),
			Resource.getString("VideoPanel.patchBitrateValue.1"),
			Resource.getString("VideoPanel.patchBitrateValue.2"),
			Resource.getString("VideoPanel.patchBitrateValue.3"),
			Resource.getString("VideoPanel.patchBitrateValue.4"),
			Resource.getString("VideoPanel.patchBitrateValue.5"),
			Resource.getString("VideoPanel.patchBitrateValue.6"),
			Resource.getString("VideoPanel.patchBitrateValue.7"),
			Resource.getString("VideoPanel.patchBitrateValue.8")
		};
		this.ITEMS_BitrateInAllSequences = ITEMS_BitrateInAllSequences;

		Object[] ITEMS_BitrateInFirstSequence = {
			Resource.getString("VideoPanel.patch1stBitrateValue.0"),
			Resource.getString("VideoPanel.patch1stBitrateValue.1"),
			Resource.getString("VideoPanel.patch1stBitrateValue.2"),
			Resource.getString("VideoPanel.patch1stBitrateValue.3"),
			Resource.getString("VideoPanel.patch1stBitrateValue.4")
		};
		this.ITEMS_BitrateInFirstSequence = ITEMS_BitrateInFirstSequence;

		Object[] ITEMS_losslessMpaConversionMode = { 
			Resource.getString("AudioPanel.losslessMpaConversionMode0"),
			Resource.getString("AudioPanel.losslessMpaConversionMode1"),
			Resource.getString("AudioPanel.losslessMpaConversionMode2"),
			Resource.getString("AudioPanel.losslessMpaConversionMode3"),
			Resource.getString("AudioPanel.losslessMpaConversionMode4"),
			Resource.getString("AudioPanel.losslessMpaConversionMode5"),
			Resource.getString("AudioPanel.losslessMpaConversionMode6")
		};
		this.ITEMS_losslessMpaConversionMode = ITEMS_losslessMpaConversionMode;

		Object[] ITEMS_resampleAudioMode = { 
			Resource.getString("AudioPanel.decodeMpgAudio.resampleAudioMode0"),
			Resource.getString("AudioPanel.decodeMpgAudio.resampleAudioMode1"),
			Resource.getString("AudioPanel.decodeMpgAudio.resampleAudioMode2")
		};
		this.ITEMS_resampleAudioMode = ITEMS_resampleAudioMode;

		Object[] ITEMS_CutMode = { 
			Resource.getString("CollectionPanel.CutMode.Bytepos"), 
			Resource.getString("CollectionPanel.CutMode.Gop"),
			Resource.getString("CollectionPanel.CutMode.Frame"),
			Resource.getString("CollectionPanel.CutMode.Pts"),
			Resource.getString("CollectionPanel.CutMode.Timecode")
		}; 
		this.ITEMS_CutMode = ITEMS_CutMode;

		Object[] ITEMS_SubtitleExportFormat = {
			Resource.getString("SubtitlePanel.Format.Free"), "SC", "SUB", "SRT", "STL", "SSA", "SON", "SUP", "SRTC", "W3C", "GPAC", "BDN", "null"
		};
		this.ITEMS_SubtitleExportFormat = ITEMS_SubtitleExportFormat;

		Object[] ITEMS_SubtitleChangeDisplay = {
			"no change", "all forced", "all not forced"
		};
		this.ITEMS_SubtitleChangeDisplay = ITEMS_SubtitleChangeDisplay;

		Object[] ITEMS_FileTypes = {
			Resource.getString("scan.unsupported"),  //0
			"PES (incl. MPEG Video)",       //1
			"MPEG-1 PS/SS (PES Container)", //2
			"MPEG-2 PS/SS (PES Container)", //3
			"PVA (TT® PES Container)",      //4
			"TS (generic PES Container)",   //5
			"PES (MPEG Audio first)",       //6
			"PES (private stream 1 first)", //7
			"ES (MPEG Video)",              //8
			"ES (MPEG Audio)",              //9
			"ES (AC-3 Audio)",              //10
			"ES (AC-3 Audio) (psb. SMPTE)", //11
			"ES (DTS Audio)",               //12
			"ES (DTS Audio) (psb. SMPTE)",  //13
			"ES (RIFF Audio)",              //14
			"ES (compressed RIFF Audio)",   //15
			"ES (Subpicture 2-bit RLE)",    //16
			"", "", "", "", "", "", "", "", "", "", "", "", "", "",  //17-30
			"PjX PTS File"                  //31 
		};
		this.ITEMS_FileTypes = ITEMS_FileTypes;
	}
}