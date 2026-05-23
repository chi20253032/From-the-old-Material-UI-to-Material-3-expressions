package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.converter.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppScreen()
            }
        }
    }
}

@Composable
fun MainAppScreen(viewModel: ConverterViewModel = viewModel()) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(0) }
    
    // Status Flow variables
    val step by viewModel.step.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val metadata by viewModel.metadata.collectAsState()
    val convertedCode by viewModel.convertedCode.collectAsState()
    val historyList by viewModel.conversionHistory.collectAsState(initial = emptyList())

    // File selection callback launcher (SAF)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.processSelectedApk(uri)
        } else {
            Toast.makeText(context, "No APK file was selected", Toast.LENGTH_SHORT).show()
        }
    }

    LiquidFlowBackground {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                GlassBottomBar {
                    GlassBottomNavigationItem(
                        selected = currentTab == 0,
                        onClick = { currentTab = 0 },
                        icon = { Icon(Icons.Default.Build, contentDescription = "Convert") },
                        label = "Convert"
                    )
                    GlassBottomNavigationItem(
                        selected = currentTab == 1,
                        onClick = { currentTab = 1 },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Playground") },
                        label = "Playground"
                    )
                    GlassBottomNavigationItem(
                        selected = currentTab == 2,
                        onClick = { currentTab = 2 },
                        icon = { Icon(Icons.Default.Info, contentDescription = "Info") },
                        label = "Info & history"
                    )
                }
            },
            contentWindowInsets = WindowInsets.safeDrawing
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
            ) {
                // Crossfade provides clean fluid screen transitions of tabs
                Crossfade(
                    targetState = currentTab,
                    animationSpec = tween(400),
                    label = "Tab Transition"
                ) { tab ->
                    when (tab) {
                        0 -> ConverterTab(
                            viewModel = viewModel,
                            step = step,
                            progress = progress,
                            logs = logs,
                            metadata = metadata,
                            convertedCode = convertedCode,
                            onPickFile = { filePickerLauncher.launch("application/vnd.android.package-archive") }
                        )
                        1 -> PlaygroundTab(
                            viewModel = viewModel
                        )
                        2 -> InfoAndHistoryTab(
                            historyList = historyList,
                            onLoadHistory = { history ->
                                viewModel.loadHistoryToView(history)
                                currentTab = 1 // Switch to playground to view interactive converted layout
                                Toast.makeText(context, "Restored layout: ${history.appName}", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConverterTab(
    viewModel: ConverterViewModel,
    step: ConversionStep,
    progress: Float,
    logs: List<String>,
    metadata: ApkMetadata?,
    convertedCode: String,
    onPickFile: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val terminalListState = rememberLazyListState()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    // Auto scroll compilation console to bottom as new tasks write
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            terminalListState.animateScrollToItem(logs.size - 1)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
    ) {
        // App Header Title
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "APK decompiler & compiler",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(GlassColors.MintAqua)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Jetpack Compose + Liquid Glass compiler engine",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // File Selection Glass Panel
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = "APK Icon Container",
                        tint = GlassColors.MintAqua,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    if (metadata == null) {
                        Text(
                            text = "No active decompilation target selected",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Select any Material 2 XML/APK to convert it",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Text(
                            text = metadata.appName,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = metadata.packageName,
                            color = GlassColors.MintAqua,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // App details specifications table
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("VERSION", fontSize = 10.sp, color = Color.White.copy(alpha = 0.4f))
                                Text(metadata.versionName, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("COMPLEXITY", fontSize = 10.sp, color = Color.White.copy(alpha = 0.4f))
                                Text(metadata.estimatedUiComplexity.substringBefore(" "), fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("TARGET SDK", fontSize = 10.sp, color = Color.White.copy(alpha = 0.4f))
                                Text("SDK ${metadata.targetSdkVersion}", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(22.dp))
                    
                    GlassButton(
                        onClick = onPickFile,
                        modifier = Modifier.fillMaxWidth(0.9f),
                        accentColor = GlassColors.MintAqua
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Folder Icon", tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (metadata == null) "Select APK file" else "Decompile New APK",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Translation Progress bar steps (Show when loading/running compile)
        if (step !is ConversionStep.Idle) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when (step) {
                                    is ConversionStep.Decompiling -> "🐳 Decompressing binaries..."
                                    is ConversionStep.Analysis -> "🔍 Layout parsing analysis..."
                                    is ConversionStep.Converting -> "🔮 Compose generation mapping..."
                                    is ConversionStep.Recompiling -> "🔨 Re-building assets..."
                                    is ConversionStep.Signing -> "🔑 Signing APK signatures..."
                                    is ConversionStep.OutputReady -> "🎉 Success! APK Compiled"
                                    is ConversionStep.Error -> "❌ Conversion Failed"
                                    else -> "Idle"
                                },
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                color = GlassColors.MintAqua,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        // Glass Styled Progress Bar Track
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = 0.1f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progress)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(GlassColors.CoralRose, GlassColors.MintAqua)
                                        )
                                    )
                            )
                        }

                        // Terminal compiler output logs visualization
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "COMPILATION CONSOLE OUTPUT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.6f))
                                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .padding(8.dp)
                        ) {
                            if (logs.isEmpty()) {
                                Text(
                                    text = "Booting isolated builder sandbox daemon...",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            } else {
                                LazyColumn(
                                    state = terminalListState,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(logs) { logLine ->
                                        Text(
                                            text = logLine,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            color = when {
                                                logLine.startsWith("✅") || logLine.contains("successful", ignoreCase = true) -> Color(0xFF00FFCC)
                                                logLine.startsWith("❌") || logLine.startsWith("⚠️") -> Color(0xFFFF4D4D)
                                                logLine.startsWith("   ├──") -> Color.White.copy(alpha = 0.85f)
                                                else -> Color.White.copy(alpha = 0.6f)
                                            },
                                            modifier = Modifier.padding(bottom = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Custom action targets upon compilation completion
        if (step is ConversionStep.OutputReady) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "COMPILED ARTIFACTS COMPLETED",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        // Code actions rows
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            GlassButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(convertedCode))
                                    Toast.makeText(context, "Compose code copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                accentColor = GlassColors.CoralRose
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Copy", tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy code", fontSize = 12.sp, color = Color.White)
                            }

                            // Save compiled APK inside the user's Downloads/ConvertedAPKs directory (Scoped Storage)
                            GlassButton(
                                onClick = {
                                    viewModel.saveRecompiledApk(context) { success, fileNameOrMessage ->
                                        if (success) {
                                            Toast.makeText(context, "APKをDownloads/ConvertedAPKsフォルダに保存しました！\n($fileNameOrMessage)", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "保存に失敗しました: $fileNameOrMessage", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                accentColor = GlassColors.MintAqua
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Save APK", tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save APK", fontSize = 12.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlaygroundTab(
    viewModel: ConverterViewModel
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
    ) {
        // Tab description
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "AI assisted migration workspace",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color.White
                )
                Text(
                    text = "Paste layout XML scripts to instantly generate modern Compose layout views.",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }

        // Scratchpad Input Block
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(4.dp)) {
                    Text(
                        text = "INPUT: LEGACY LAYOUT XML",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    GlassTextField(
                        value = viewModel.scratchpadInput,
                        onValueChange = { viewModel.scratchpadInput = it },
                        placeholder = "Paste XML layout block here...",
                        modifier = Modifier.fillMaxWidth().height(140.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        GlassButton(
                            onClick = { viewModel.convertScratchpadCode() },
                            enabled = !viewModel.isTranslatingScratchpad,
                            accentColor = GlassColors.MintAqua
                        ) {
                            if (viewModel.isTranslatingScratchpad) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                            } else {
                                Icon(Icons.Default.PlayArrow, contentDescription = "AI", tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Decompile with Gemini", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Scratchpad Output Block
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "OUTPUT: COMPOSE LIQUID GLASS CODE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                        IconButton(
                            onClick = {
                                if (viewModel.scratchpadOutput.isNotBlank()) {
                                    clipboardManager.setText(AnnotatedString(viewModel.scratchpadOutput))
                                    Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Copy text", tint = Color.White.copy(alpha = 0.6f))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Code highlight scrolling container
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.45f))
                            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
                            .padding(10.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = viewModel.scratchpadOutput.ifBlank { "// Your newly translated Compose code will print here." },
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = if (viewModel.scratchpadOutput.isBlank()) Color.Gray else Color(0xFF00FFCC)
                        )
                    }
                }
            }
        }

        // Active Liquid Glass Preview Rendering Context
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "LIVE GRAPHICS PREVIEW",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                )

                // Renders the translated items inside our custom Liquid Glass card containers
                if (viewModel.scratchpadOutput.isBlank()) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No active previews render. Compile any XML layout inside the decompiler to see live renders.",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.4f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    // Direct dynamic compiler preview mock container
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderWidth = 1.dp,
                        borderGradient = Brush.linearGradient(listOf(GlassColors.GlassBorderAccent, Color.Transparent, GlassColors.GlassBorderLight))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Converted Glass Screen Frame",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlassColors.MintAqua,
                                modifier = Modifier.align(Alignment.Start)
                            )

                            // Showcase a beautiful Live liquid glass composition
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column {
                                    Text("Welcome Back Controller", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Manage your connected services in real-time.", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                                }
                            }

                            var sampleInputValue by remember { mutableStateOf("") }
                            GlassTextField(
                                value = sampleInputValue,
                                onValueChange = { sampleInputValue = it },
                                placeholder = "Enter credentials API endpoint",
                                modifier = Modifier.fillMaxWidth()
                            )

                            GlassButton(
                                onClick = {
                                    Toast.makeText(context, "Interacted with newly recompiled Glass button component!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                accentColor = GlassColors.MintAqua
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Active", tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Establish Encrypted Connection", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoAndHistoryTab(
    historyList: List<HistoryEntity>,
    onLoadHistory: (HistoryEntity) -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
    ) {
        // App limitation and documentation panel
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "注意点と制限事項 (Important Notices)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color.White
                )
                Text(
                    text = "Understanding automated decompiling-to-Compose mappings and scopes on physical mobile targets.",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }

        // Interactive Warnings Grid
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "OK", tint = GlassColors.MintAqua)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("完全自動化が可能な領域 (Fully Automated Mappings)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    }
                    Text(
                        text = "・ ほとんどの標準XMLレイアウト（LinearLayout, FrameLayout, RelativeLayout）の変換。\n" +
                               "・ 基本的なマテリアルボタン、入力フォーム、スタック型カードビューの実装。\n" +
                               "・ リソースベースのカラーカラーパレットの自動 derivation と Compose テーマ統合。",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.70f),
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(start = 32.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = "Warning", tint = Color(0xFFFFB703))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("手動調整や移行フォローが必要な領域 (Manual Refactoring Needed)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    }
                    Text(
                        text = "・ デフォルトでない非推奨のサードパーティ独自カスタムビューライブラリ。\n" +
                               "・ バックグラウンドのハードウェアドライバとの接続、非同期インフラバインダーの記述。\n" +
                               "・ 複雑なスタックのナビゲーショングラフ設計、DI(Dependency Injection)構造のマッピング。",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.70f),
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(start = 32.dp)
                    )
                }
            }
        }

        // Security guidelines card
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundGradient = Brush.verticalGradient(listOf(Color(0x20F72585), Color(0x06060614))),
                borderGradient = Brush.horizontalGradient(listOf(GlassColors.GlassBorderLight, GlassColors.NeonPink.copy(alpha = 0.6f)))
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = "Security", tint = GlassColors.NeonPink)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Security API Key Information & Warning", fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Security Warning: I have included your API keys in the generated APK file for this prototype. Please be aware that Android APKs can be easily decompiled, and these keys can be extracted by anyone who has access to the file. Do not share this APK file publicly or with unauthorized individuals to prevent potential misuse.",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // History items index from Room database
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CONVERSION ARCHIVES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.padding(start = 4.dp)
                )
                Text(
                    text = "Room Persistence",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlassColors.MintAqua
                )
            }
        }

        if (historyList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No past conversion jobs registered locally.",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.35f)
                    )
                }
            }
        } else {
            items(historyList) { history ->
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onLoadHistory(history) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = history.appName,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${history.packageName} • v${history.versionName}",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp
                            )
                        }
                        
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${history.accuracyScore}% Match",
                                color = if (history.accuracyScore > 90) Color(0xFF00FFCC) else Color(0xFFFFB703),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = java.text.SimpleDateFormat("MM/dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(history.conversionTime)),
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
