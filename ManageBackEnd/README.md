# arXiv论文管理后台

基于Vue3 + TypeScript + Element Plus构建的arXiv论文管理后台系统。

## 技术栈

- **Vue 3**: 渐进式JavaScript框架
- **TypeScript**: JavaScript的超集，提供类型安全
- **Element Plus**: 基于Vue 3的组件库
- **Vite**: 新一代前端构建工具
- **Vue Router**: Vue.js官方路由管理器
- **Axios**: HTTP客户端

## 项目结构

```
ManageBackEnd/
├── src/
│   ├── api/              # API接口封装
│   │   └── arxiv.ts      # arXiv相关API
│   ├── assets/           # 静态资源
│   ├── components/       # 公共组件
│   ├── layout/           # 布局组件
│   │   └── MainLayout.vue # 主布局
│   ├── router/           # 路由配置
│   │   └── index.ts      # 路由定义
│   ├── types/            # TypeScript类型定义
│   │   ├── arxiv.ts      # arXiv相关类型
│   │   └── index.ts      # 通用类型
│   ├── utils/            # 工具函数
│   │   └── request.ts    # Axios封装
│   ├── views/            # 页面组件
│   │   ├── DataSync.vue  # 数据同步页面
│   │   ├── PaperEdit.vue # 论文编辑页面
│   │   └── PaperList.vue # 论文列表页面
│   ├── App.vue           # 根组件
│   └── main.ts           # 入口文件
├── index.html            # HTML模板
├── package.json          # 项目配置
├── tsconfig.json         # TypeScript配置
├── vite.config.ts        # Vite配置
└── README.md             # 项目说明
```

## 功能特性

### 1. 论文列表管理
- 多条件搜索：关键词、分类、日期范围
- 分页显示论文列表
- 查看论文详细信息
- 编辑论文信息
- 快速打开arXiv页面或下载PDF

### 2. 数据同步功能
- **按分类同步**: 选择arXiv分类获取最新论文
- **按日期范围同步**: 指定日期范围获取论文
- **按关键词同步**: 根据关键词搜索并获取论文
- **最近论文同步**: 获取最近N天的论文
- 同步日志记录和显示

### 3. 论文编辑
- 编辑论文基本信息
- 修改分类、标题、作者等字段
- 保存更新

## 安装和运行

### 安装依赖

```bash
cd ManageBackEnd
npm install
```

### 开发模式

```bash
npm run dev
```

访问 http://localhost:3001

### 生产构建

```bash
npm run build
```

### 预览生产构建

```bash
npm run preview
```

## API接口配置

管理后台通过调用后端API接口来获取和更新数据，接口配置在 `src/utils/request.ts` 中：

- **baseURL**: `/api`
- **proxy**: 代理到 `http://localhost:8080`（后端服务地址）

## 主要页面

### 论文列表页面 (`/papers`)
- 显示论文列表表格
- 支持多条件搜索
- 分页浏览
- 查看、编辑、打开arXiv页面等操作

### 论文编辑页面 (`/papers/:id/edit`)
- 编辑论文详细信息
- 表单验证
- 保存更新

### 数据同步页面 (`/sync`)
- 四种同步方式
- 实时日志显示
- 同步结果反馈

## 注意事项

1. 确保后端服务已启动并运行在 `http://localhost:8080`
2. 前端开发服务器运行在 `http://localhost:3001`
3. 使用代理转发API请求到后端
4. 所有代码注释使用中文
5. 数据库字段命名使用 `created_time` 和 `updated_time`
