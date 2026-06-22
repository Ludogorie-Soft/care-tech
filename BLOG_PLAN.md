# План: Блог функционалност

## Архитектурен overview

- Статиите се управляват само от администратора (ADMIN/SUPER_ADMIN)
- Публичните GET ендпойнти са без автентикация
- Снимки (корица + вградени в съдържанието) се качват в S3 чрез съществуващия `FileUploadController`
- Flyway следва текущата версия — следващата миграция е **V27**

---

## ФАЗА 1 — Backend

### 1. Flyway миграция — `V27__add_blog_posts.sql`

```sql
CREATE TABLE blog_posts (
    id               BIGSERIAL PRIMARY KEY,
    title            VARCHAR(255) NOT NULL,
    slug             VARCHAR(255) NOT NULL UNIQUE,
    excerpt          TEXT,
    content          TEXT NOT NULL,
    cover_image_url  VARCHAR(500),
    published        BOOLEAN NOT NULL DEFAULT false,
    published_at     TIMESTAMP,
    created_by       VARCHAR(255),
    last_modified_by VARCHAR(255),
    created_at       TIMESTAMP DEFAULT now(),
    last_modified_at TIMESTAMP DEFAULT now()
);

CREATE INDEX idx_blog_posts_slug ON blog_posts(slug);
CREATE INDEX idx_blog_posts_published ON blog_posts(published, published_at DESC);
```

### 2. `BlogPost.java` entity — `src/main/java/com/techstore/entity/`
- Extends `BaseEntity` (audit колони наследени)
- Полета: `title`, `slug`, `excerpt`, `content` (HTML), `coverImageUrl`, `published`, `publishedAt`

### 3. DTOs
- `BlogPostRequestDto.java` — за create/update (title, slug, excerpt, content, coverImageUrl, published)
- `BlogPostResponseDto.java` — за response (всички полета + id, createdAt, publishedAt)
- Slug се генерира автоматично в service-а от title ако не е подаден

### 4. `BlogPostRepository.java`
- `Page<BlogPost> findByPublishedTrueOrderByPublishedAtDesc(Pageable pageable)`
- `Optional<BlogPost> findBySlug(String slug)`
- `boolean existsBySlug(String slug)`

### 5. `BlogPostService.java`
- `createPost(BlogPostRequestDto)` — генерира slug, задава publishedAt ако published=true
- `updatePost(Long id, BlogPostRequestDto)` — ако slug се промени, проверява уникалност
- `deletePost(Long id)` — изтрива и cover image от S3
- `getPublishedPosts(Pageable)` — само публикувани, наредени по дата
- `getAllPosts(Pageable)` — за admin панела (включва чернови)
- `getBySlug(String slug)` — за публичната страница
- `getById(Long id)` — за admin edit
- Slug генерация: title → lowercase → replace spaces/special chars → Cyrillic транслитерация

### 6. `BlogPostController.java` — `/api/blog` и `/api/admin/blog`

| Method | Endpoint             | Auth  | Описание                        |
|--------|----------------------|-------|---------------------------------|
| GET    | `/api/blog`          | public | Публикувани статии (pagination) |
| GET    | `/api/blog/{slug}`   | public | Единична статия по slug         |
| GET    | `/api/admin/blog`    | ADMIN | Всички статии (вкл. чернови)    |
| GET    | `/api/admin/blog/{id}` | ADMIN | Статия по ID                  |
| POST   | `/api/admin/blog`    | ADMIN | Създаване                       |
| PUT    | `/api/admin/blog/{id}` | ADMIN | Редактиране                   |
| DELETE | `/api/admin/blog/{id}` | ADMIN | Изтриване                     |

### 7. `SecurityConfig.java` — добавяне
```java
.requestMatchers(HttpMethod.GET, "/api/blog/**").permitAll()
// /api/admin/blog/** вече е покрит от .requestMatchers("/api/admin/**")
```

---

## ФАЗА 2 — Frontend Admin

### 8. `blogSlice.js` — `care-tech-ui/src/redux/`
- Thunks: `fetchAllBlogPosts`, `fetchBlogPostById`, `createBlogPost`, `updateBlogPost`, `deleteBlogPost`
- State: `posts`, `currentPost`, `status`, `totalPages`, `currentPage`

### 9. `BlogLayout.jsx` — `care-tech-ui/src/pages/admin/Blog/`
- Таблица с колони: Заглавие, Slug, Статус (Публикувана/Чернова), Дата, Действия
- Бутон „Нова статия"
- Пагинация

### 10. `BlogPostModal.jsx` — `care-tech-ui/src/pages/admin/Blog/`
- Форма с полета:
  - **Заглавие** — text input (onChange → auto-генерира slug)
  - **Slug** — editable text input
  - **Резюме (excerpt)** — textarea (~2-3 изречения за preview)
  - **Съдържание** — textarea за HTML; бутон „Добави снимка" → upload към S3 → вмъква `<img src="...">` тага
  - **Корица** — file picker → upload към S3, preview
  - **Публикувана** — toggle/checkbox
- Submit → `createBlogPost` или `updateBlogPost`

### 11. `Sidebar.jsx` — добавяне на нов линк
```jsx
{ name: "Блог", icon: <FiFileText />, path: "/admin/dashboard/blog" }
```

### 12. `App.js` — admin route
```jsx
<Route path="/admin/dashboard/blog" element={<BlogLayout />} />
```

---

## ФАЗА 3 — Frontend Public

### 13. `Blog.jsx` — пренаписване на placeholder страницата
- Grid от статии: корица + заглавие + excerpt + дата + линк „Прочети повече"
- Pagination (10 статии на страница)
- SEO компонент

### 14. `BlogPostPage.jsx` — нов файл `care-tech-ui/src/pages/`
- Route: `/blog/:slug`
- Показва: cover image, заглавие, дата, HTML content
- Breadcrumbs: Начало → Блог → [Заглавие]

### 15. `App.js` — public routes
```jsx
<Route path="/blog" element={<Blog title="Блог" />} />
<Route path="/blog/:slug" element={<BlogPostPage />} />
```

---

## ФАЗА 4 — Качване на снимки

- Корица: `POST /api/upload/blog-cover` → S3 key: `blog/covers/{uuid}.ext`
- Inline снимки: `POST /api/upload/blog-image` → S3 key: `blog/images/{uuid}.ext`
- При изтриване на статия → изтриване само на cover image от S3

---

## Ред на изпълнение

```
[x] 1.  V27 миграция
[x] 2.  BlogPost entity
[x] 3.  BlogPostRequestDto + BlogPostResponseDto
[x] 4.  BlogPostRepository
[x] 5.  BlogPostService
[x] 6.  BlogPostController
[x] 7.  SecurityConfig update
[x] 8.  FileUploadController — нови blog endpoints
[x] 9.  BUILD CHECK ✓
[x] 10. blogSlice.js
[x] 11. BlogLayout.jsx + BlogPostModal.jsx
[x] 12. Sidebar.jsx + App.js (admin route)
[x] 13. Blog.jsx (пренаписване)
[x] 14. BlogPostPage.jsx
[x] 15. App.js (public routes)

## Бележки от имплементацията

- ComingSoonPage.jsx извлечен от стария Blog.jsx — placeholder за /delivery-and-payment, /return-policy, /installment-purchase, /device-insurance, /faq
- Breadcrumbs компонентът очаква { name, url }, не { label, href }
- BaseEntity използва updated_at (не last_modified_at) — V27 поправен преди commit
- Blog slug генерира се в BlogPostService (не в entity) за по-добър контрол при update/uniqueness check
```
