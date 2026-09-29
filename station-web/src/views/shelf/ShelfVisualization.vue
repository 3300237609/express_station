<template>
  <div class="shelf-visualization">
    <!-- 顶部工具栏 -->
    <div class="toolbar">
      <el-select
        v-model="selectedShelfId"
        placeholder="选择货架"
        style="width: 220px"
        @change="loadGrid"
      >
        <el-option label="全部货架" :value="null" />
        <el-option v-for="s in shelfOptions" :key="s.id" :label="`${s.shelfNo} · ${s.area}`" :value="s.id" />
      </el-select>

      <el-input
        v-model="searchNo"
        placeholder="输入快递单号搜索定位"
        style="width: 280px"
        :prefix-icon="Search"
        clearable
        @keyup.enter="handleSearch"
        @clear="clearSearch"
      />
      <el-button type="primary" @click="handleSearch" :loading="searching">搜索</el-button>
      <el-button @click="loadGrid" :icon="Refresh">刷新</el-button>

      <div class="legend" v-if="locations.length">
        <span class="legend-item"><span class="dot dot-idle" />空闲</span>
        <span class="legend-item"><span class="dot dot-partial" />部分占用</span>
        <span class="legend-item"><span class="dot dot-full" />已满</span>
        <span class="legend-item"><span class="dot dot-parcel" />在架快件</span>
        <span class="legend-item"><span class="dot dot-disabled" />停用</span>
        <span class="legend-item"><span class="dot dot-placeholder" />未建货位</span>
      </div>
    </div>

    <!-- 卡片区域：货架卡片并排平铺（自动换行） -->
    <div class="card-grid" v-loading="loading">
      <div v-if="locations.length === 0 && !loading" class="empty-tip">暂无货位数据</div>

      <div v-for="group in groupedShelves" :key="group.shelfId" class="shelf-card">
        <div class="card-header">
          <span class="card-title">{{ group.shelfNo }} · {{ group.area }}</span>
          <el-tag size="small" type="warning" effect="plain">
            {{ packageCountInGroup(group) }} 件
          </el-tag>
        </div>
        <div class="card-sub">
          {{ group.maxRow }} 行 × {{ group.maxCol }} 列 · 已建 {{ group.locations.length }} 货位
        </div>
        <div class="card-body">
          <svg
            :viewBox="viewBoxFor(group)"
            class="shelf-svg"
            :style="{ maxWidth: naturalWidth(group) + 'px' }"
            @mousemove="onSvgMove"
            @mouseleave="onSvgLeave"
          >
            <!-- 拟物渐变定义（id 加 shelfId 后缀，避免多卡片同 id 冲突） -->
            <defs>
              <linearGradient :id="`shellIdle${group.shelfId}`" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0" stop-color="#fcfdfe" /><stop offset="1" stop-color="#d6dbe1" />
              </linearGradient>
              <linearGradient :id="`shellPartial${group.shelfId}`" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0" stop-color="#fff4da" /><stop offset="1" stop-color="#edb156" />
              </linearGradient>
              <linearGradient :id="`shellFull${group.shelfId}`" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0" stop-color="#e85a4c" /><stop offset="1" stop-color="#b8241a" />
              </linearGradient>
              <linearGradient :id="`cavity${group.shelfId}`" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0" stop-color="#c2c7cf" /><stop offset="1" stop-color="#a8aeb8" />
              </linearGradient>
              <linearGradient :id="`cavityOff${group.shelfId}`" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0" stop-color="#e8eaee" /><stop offset="1" stop-color="#cfd3d9" />
              </linearGradient>
              <linearGradient :id="`boxHot${group.shelfId}`" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0" stop-color="#fff9de" /><stop offset="1" stop-color="#ffcf4a" />
              </linearGradient>
            </defs>

            <g v-for="cell in group.grid" :key="`${cell.row}-${cell.col}`">
              <!-- ===== 有货位记录：渲染拟物容器 ===== -->
              <template v-if="cell.loc">
                <!-- 投影 -->
                <rect class="bin-shadow" :x="cell.ox + 1.5" :y="cell.oy + 2.5"
                      :width="CELL_W - GAP" :height="CELL_H" rx="5" />
                <!-- 容器外壁（填充=状态渐变） -->
                <rect
                  :class="['bin-shell', `bin-${statusKey(cell.loc)}`, {
                    blink: blinkLocationId === cell.loc.id
                  }]"
                  :x="cell.ox" :y="cell.oy"
                  :width="CELL_W - GAP" :height="CELL_H" rx="5"
                  :fill="shellFill(cell.loc, group.shelfId)"
                />
                <!-- 顶部高光 -->
                <rect v-if="cell.loc.status !== 4" class="bin-highlight"
                      :x="cell.ox + 4" :y="cell.oy + 3"
                      :width="CELL_W - GAP - 8" height="4" rx="2" />
                <!-- 内部空腔（停用容器用浅灰空腔） -->
                <rect class="bin-cavity"
                      :x="cell.ox + 5" :y="cell.oy + 9"
                      :width="CELL_W - GAP - 10" :height="26" rx="3"
                      :fill="cell.loc.status === 4
                        ? `url(#cavityOff${group.shelfId})`
                        : `url(#cavity${group.shelfId})`" />
                <!-- 空腔内的快件小方格（按 used_count 摆放，有方格=有快件=待取，最多显示 6 个） -->
                <g v-for="(b, i) in parcelBoxes(cell.ox, cell.oy, cell.loc.usedCount)" :key="i">
                  <rect
                    class="parcel"
                    :x="b.x" :y="b.y" width="10" height="9" rx="1.5"
                    :fill="`url(#boxHot${group.shelfId})`"
                  />
                </g>
                <!-- 超过 6 件显示 +N -->
                <text v-if="(cell.loc.usedCount ?? 0) > 6" class="parcel-more"
                      :x="cell.ox + (CELL_W - GAP) / 2" :y="cell.oy + 27" text-anchor="middle"
                >+{{ cell.loc.usedCount - 6 }}</text>
                <!-- 容器编号标签 -->
                <text class="bin-label"
                      :x="cell.ox + (CELL_W - GAP) / 2" :y="cell.oy + CELL_H + 9"
                      text-anchor="middle"
                >{{ shortLocationNo(cell.loc.locationNo) }}</text>
                <!-- 透明点击热区（覆盖容器+标签） -->
                <rect class="hit-area"
                      :x="cell.ox" :y="cell.oy"
                      :width="CELL_W - GAP" :height="CELL_H + LABEL_H"
                      @click="onCellClick(cell.loc)"
                      @mouseenter="hoveredLocation = cell.loc"
                      @mouseleave="hoveredLocation = null" />
              </template>

              <!-- ===== 无记录：虚线占位（只占容器位，下方对齐标签高度） ===== -->
              <rect v-else class="cell-placeholder"
                    :x="cell.ox" :y="cell.oy"
                    :width="CELL_W - GAP" :height="CELL_H" rx="4" />
            </g>
          </svg>
        </div>
      </div>

      <!-- Tooltip -->
      <div v-if="hoveredLocation && tooltipPos.show" class="tooltip" :style="tooltipPos">
        <div><b>{{ hoveredLocation.locationNo }}</b> <span class="sub">{{ hoveredLocation.shelfNo }}</span></div>
        <div>行 {{ hoveredLocation.rowNo }} · 列 {{ hoveredLocation.columnNo }}</div>
        <div>容量 {{ hoveredLocation.capacity }} / 已用 {{ hoveredLocation.usedCount ?? 0 }}</div>
        <div>状态：{{ statusText(hoveredLocation.status) }}</div>
      </div>
    </div>

    <!-- 点击货位弹窗 -->
    <el-dialog v-model="dialogVisible" :title="`货位 ${dialogData?.locationNo} · 快件信息`" width="520px">
      <div v-if="dialogData">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="货架">{{ dialogData.shelfNo }} · {{ dialogData.area }}</el-descriptions-item>
          <el-descriptions-item label="货位">{{ dialogData.locationNo }}（行 {{ dialogData.rowNo }} · 列 {{ dialogData.columnNo }}）</el-descriptions-item>
          <el-descriptions-item label="容量">{{ dialogData.capacity }} / 已用 {{ dialogData.usedCount ?? 0 }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTagType(dialogData.status)">{{ statusText(dialogData.status) }}</el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">在架快件</el-divider>
        <el-table :data="dialogPendingExpress" stripe size="small" style="width: 100%">
          <el-table-column prop="expressNo" label="快递单号" min-width="180" />
          <el-table-column prop="receiverName" label="收件人" width="90" />
          <el-table-column prop="pickupCode" label="取件码" width="90" />
          <el-table-column prop="arrivalTime" label="到达时间" width="160" />
        </el-table>
        <el-empty v-if="dialogPendingExpress.length === 0" description="该货位暂无在架快件" :image-size="60" />
      </div>
      <template #footer>
        <el-button @click="dialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { gridFull, listShelf } from '@/api/shelf'
import { detailExpressByNo, listExpress } from '@/api/express'

// ========== 常量（拟物容器网格） ==========
const CELL_W = 50          // 格宽（水平步进）
const CELL_H = 40          // 容器高度
const LABEL_H = 11         // 容器下方编号标签高度
const STEP_Y = CELL_H + LABEL_H + 3   // 垂直步进 54
const GAP = 3              // 水平间距
const PADDING = 12         // SVG 内边距

// ========== 状态 ==========
const selectedShelfId = ref(null)   // null = 全部货架
const shelfOptions = ref([])
const locations = ref([])
const pendingExpressMap = ref({})
const loading = ref(false)

const searchNo = ref('')
const searching = ref(false)
const blinkLocationId = ref(null)

const hoveredLocation = ref(null)
const tooltipPos = ref({ show: false, left: 0, top: 0 })

const dialogVisible = ref(false)
const dialogData = ref(null)
const dialogPendingExpress = ref([])

// ========== 分组 + 完整矩阵生成 ==========
const groupedShelves = computed(() => {
  const map = new Map()
  for (const loc of locations.value) {
    const sid = loc.shelfId
    if (!map.has(sid)) {
      map.set(sid, {
        shelfId: sid,
        shelfNo: loc.shelfNo || '?',
        area: loc.area || '-',
        locations: []
      })
    }
    map.get(sid).locations.push(loc)
  }
  // 货架按 shelfId 升序；每个货架按 maxRow × maxCol 生成完整网格
  return Array.from(map.values())
    .sort((a, b) => a.shelfId - b.shelfId)
    .map(g => {
      const sorted = g.locations.sort((a, b) => (a.rowNo - b.rowNo) || (a.columnNo - b.columnNo))
      // 以已存在货位的最大行列号作为货架矩阵尺寸（行、列至少为 1）
      const maxRow = Math.max(1, ...sorted.map(l => l.rowNo || 0))
      const maxCol = Math.max(1, ...sorted.map(l => l.columnNo || 0))
      // row-col → 真实货位记录
      const locMap = new Map(sorted.map(l => [`${l.rowNo}-${l.columnNo}`, l]))
      // 按行优先展开完整矩阵：每个 cell 预计算 SVG 原点坐标 ox/oy；缺记录 loc=null 渲染占位
      const grid = []
      for (let r = 1; r <= maxRow; r++) {
        for (let c = 1; c <= maxCol; c++) {
          grid.push({
            row: r, col: c,
            ox: PADDING + (c - 1) * CELL_W,
            oy: PADDING + (r - 1) * STEP_Y,
            loc: locMap.get(`${r}-${c}`) || null
          })
        }
      }
      return { ...g, locations: sorted, maxRow, maxCol, grid }
    })
})

// ========== SVG 尺寸 ==========
function naturalWidth(group) {
  return PADDING * 2 + group.maxCol * CELL_W
}
function naturalHeight(group) {
  // 每行占 STEP_Y，最后一行只占容器+标签（不含尾部行间隙）
  return PADDING * 2 + (group.maxRow - 1) * STEP_Y + CELL_H + LABEL_H
}
function viewBoxFor(group) {
  return `0 0 ${naturalWidth(group)} ${naturalHeight(group)}`
}

// ========== 格内简写编号：如 A-01-01 → 01-01（完整编号看 tooltip/弹窗） ==========
function shortLocationNo(no) {
  if (!no) return ''
  const parts = String(no).split('-')
  return parts.length > 2 ? parts.slice(-2).join('-') : no
}

// ========== 容器状态 ==========
/** 货架内快件总数（各货位 used_count 求和；有件即待取） */
const packageCountInGroup = (group) =>
  group.locations.reduce((sum, l) => sum + (l.usedCount ?? 0), 0)
/** 状态 → class 后缀（idle/partial/full/disabled） */
function statusKey(loc) {
  if (loc.status === 4) return 'disabled'
  if (loc.status === 3) return 'full'
  if (loc.status === 2) return 'partial'
  return 'idle'
}
/** 状态 → 外壁填充渐变（停用为纯色） */
function shellFill(loc, shelfId) {
  if (loc.status === 4) return '#ececec'
  return `url(#shell${statusKey(loc).charAt(0).toUpperCase() + statusKey(loc).slice(1)}${shelfId})`
}
/**
 * 空腔内快件包裹坐标：每行 3 个、最多 2 行 6 个，从左下角往上堆叠
 * 空腔：x=ox+5, y=oy+9, w=37, h=26
 */
function parcelBoxes(ox, oy, count) {
  const n = Math.min(count ?? 0, 6)
  const boxes = []
  for (let i = 0; i < n; i++) {
    const r = Math.floor(i / 3)   // 0=底行, 1=上行
    const c = i % 3
    boxes.push({ x: ox + 7 + c * 12, y: oy + 23 - r * 11 })
  }
  return boxes
}
const statusText = (s) => ({ 1: '空闲', 2: '部分占用', 3: '已满', 4: '停用' }[s] || '-')
const statusTagType = (s) => ({ 1: 'info', 2: 'warning', 3: 'danger', 4: '' }[s] || 'info')

// ========== 生命周期 ==========
onMounted(async () => {
  try {
    const { data } = await listShelf({ pageNum: 1, pageSize: 500, status: 1 })
    shelfOptions.value = data || []
    // 默认「全部货架」
    await loadGrid()
  } catch (_) { /* ignore */ }
})

// ========== 加载网格 ==========
async function loadGrid() {
  loading.value = true
  try {
    const gridRes = await gridFull(selectedShelfId.value ?? undefined)
    locations.value = gridRes.data || []
    await loadPendingExpress()
  } catch (err) {
    console.error('加载货架网格失败', err)
  } finally {
    loading.value = false
  }
}

async function loadPendingExpress() {
  pendingExpressMap.value = {}
  // 取件出库会释放货位（location_id 置空），所以凡带 locationId 的都是在架快件
  // 不传 status 拉全量，直接按 locationId 分组（含待取/异常等全部在架件）
  const res = await listExpress({ keyword: '', pageNum: 1, pageSize: 5000 })
  const list = res.data || []
  list.forEach((e) => {
    if (e.locationId) {
      const bucket = pendingExpressMap.value[e.locationId] || (pendingExpressMap.value[e.locationId] = [])
      bucket.push(e)
    }
  })
}

// ========== 搜索定位 ==========
async function handleSearch() {
  const no = searchNo.value?.trim()
  if (!no) return
  searching.value = true
  try {
    const { data } = await detailExpressByNo(no)
    if (!data) { ElMessage.warning('未找到该快递'); return }
    // 若当前选的货架与快递所在不一致，自动切换
    if (data.shelfId && data.shelfId !== selectedShelfId.value) {
      selectedShelfId.value = data.shelfId
      await loadGrid()
    }
    if (data.locationId) {
      blinkLocationId.value = data.locationId
      setTimeout(() => { blinkLocationId.value = null }, 2500)
      ElMessage.success(`已定位：${data.shelfNo} · 货位 ${data.locationNo}`)
    } else {
      ElMessage.info('该快递尚未分配货位')
    }
  } catch (_) {
    ElMessage.error('搜索失败')
  } finally {
    searching.value = false
  }
}
function clearSearch() { blinkLocationId.value = null }

// ========== Tooltip ==========
function onSvgMove(e) {
  if (!hoveredLocation.value) return
  tooltipPos.value = {
    show: true,
    left: e.clientX + 16 + 'px',
    top: e.clientY + 'px'
  }
}
function onSvgLeave() {
  hoveredLocation.value = null
  tooltipPos.value.show = false
}

// ========== 点击弹窗 ==========
function onCellClick(loc) {
  dialogData.value = loc
  dialogPendingExpress.value = pendingExpressMap.value[loc.id] || []
  dialogVisible.value = true
}
</script>

<style scoped>
.shelf-visualization {
  padding: 16px;
  background: #fff;
  border-radius: 8px;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}
.legend {
  margin-left: auto;
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: #666;
}
.legend-item { display: inline-flex; align-items: center; gap: 6px; }
.dot { width: 14px; height: 14px; border-radius: 3px; display: inline-block; }
.dot-idle      { background: #f2f4f7; border: 1px solid #dcdfe6; }
.dot-partial   { background: #fdf6ec; border: 1px solid #e6a23c; }
.dot-full      { background: #c93326; border: 1px solid #8f1c14; }
.dot-parcel     { background: linear-gradient(#fff9de, #ffcf4a); border: 1px solid #d99a3a; }
.dot-disabled  { background: #f0f0f0; border: 1px dashed #c0c4cc; }
.dot-placeholder { background: repeating-linear-gradient(45deg, #f7f8fa, #f7f8fa 4px, #eef0f3 4px, #eef0f3 8px); border: 1px dashed #d8dbe0; }

/* 卡片网格：自动按容器宽度并排平铺，放不下自动换行 */
.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 16px;
  min-height: 200px;
  position: relative;
}
.empty-tip {
  grid-column: 1 / -1;
  text-align: center;
  color: #909399;
  padding: 60px 0;
  font-size: 14px;
}

/* 货架卡片 */
.shelf-card {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 14px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  transition: box-shadow 0.2s, transform 0.2s;
  display: flex;
  flex-direction: column;
}
.shelf-card:hover {
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.09);
  transform: translateY(-2px);
}
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.card-title { font-weight: 600; color: #303133; font-size: 14px; }
.card-sub { color: #909399; font-size: 12px; margin: 6px 0 10px; }

.card-body {
  overflow: auto;
  max-height: 420px;
}
.shelf-svg {
  display: block;
  width: 100%;
  height: auto;
  margin: 0 auto;
  background: #fafbfc;
  border-radius: 4px;
}

.cell { cursor: pointer; }

/* ===== 拟物容器 ===== */
.bin-shadow { fill: rgba(15, 23, 42, 0.16); }
.bin-shell { stroke-width: 1.2; transition: stroke 0.15s; }
.bin-idle     { stroke: #b8bfc8; }
.bin-partial  { stroke: #cd8d2c; }
.bin-full     { stroke: #8f1c14; }
.bin-disabled { stroke: #b2b2b2; stroke-dasharray: 4 3; }
.bin-highlight { fill: rgba(255, 255, 255, 0.55); pointer-events: none; }
.bin-cavity { pointer-events: none; }

/* 空腔内快件小方格 */
.parcel { stroke: #d99a3a; stroke-width: 0.8; pointer-events: none; }
.parcel-more {
  font-size: 9px;
  font-weight: 700;
  fill: #4a4f57;
  pointer-events: none;
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

/* 容器编号标签 */
.bin-label {
  font-size: 9px;
  fill: #5a6068;
  pointer-events: none;
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

/* 透明点击热区 */
.hit-area { fill: transparent; cursor: pointer; }

.cell-placeholder {
  fill: #f7f8fa;
  stroke: #d8dbe0;
  stroke-dasharray: 3 3;
  cursor: default;
}

.blink { animation: blink 0.4s ease-in-out 4; transform-box: fill-box; transform-origin: center; }
@keyframes blink {
  0%, 100% { opacity: 1; }
  50%      { opacity: 0.25; }
}

.tooltip {
  position: fixed;
  pointer-events: none;
  background: rgba(0, 0, 0, 0.75);
  color: #fff;
  font-size: 12px;
  padding: 8px 12px;
  border-radius: 4px;
  line-height: 1.6;
  z-index: 9999;
  white-space: nowrap;
}
.tooltip .sub { color: #cfd3dc; font-size: 11px; margin-left: 6px; }
</style>
