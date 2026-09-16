# JpaRepository 常用自訂語法整理

Spring Data JPA 的自訂查詢語法主要分成三大類。

---

## 一、方法命名查詢（Derived Query Methods）

Spring 會直接解析方法名稱自動產生查詢，不用寫 SQL。

### 基本前綴（動詞）

```java
// 查詢
List<User> findByName(String name);
// 是否存在
boolean existsByEmail(String email);
// 計數
long countByStatus(String status);
// 刪除（通常搭配 @Transactional）
void deleteByName(String name);
```

### 條件關鍵字

```java
// And / Or
List<User> findByNameAndAge(String name, int age);
List<User> findByNameOrEmail(String name, String email);

// 比較
List<User> findByAgeGreaterThan(int age);        // >
List<User> findByAgeLessThanEqual(int age);      // <=
List<User> findByAgeBetween(int start, int end); // between

// 模糊查詢
List<User> findByNameLike(String pattern);         // 要自己帶 %
List<User> findByNameContaining(String keyword);   // 自動包 %keyword%
List<User> findByNameStartingWith(String prefix);
List<User> findByNameEndingWith(String suffix);

// null 判斷
List<User> findByEmailIsNull();
List<User> findByEmailIsNotNull();

// 集合
List<User> findByIdIn(List<Long> ids);

// 布林
List<User> findByActiveTrue();
List<User> findByActiveFalse();

// 排序
List<User> findByStatusOrderByCreatedAtDesc(String status);

// 忽略大小寫
List<User> findByNameIgnoreCase(String name);
```

### 限制筆數

```java
User findFirstByOrderByCreatedAtDesc();     // 取第一筆
List<User> findTop5ByOrderByAgeDesc();       // 取前 5 筆
```

---

## 二、@Query 註解（自己寫查詢）

當方法命名太長或邏輯複雜時，改用 `@Query`。

### JPQL（操作 Entity，不是資料表）

```java
// 注意：User 是 Entity 類別名，不是 table 名
@Query("SELECT u FROM User u WHERE u.name = :name")
List<User> searchByName(@Param("name") String name);

// 位置參數（較不推薦，可讀性差）
@Query("SELECT u FROM User u WHERE u.age > ?1")
List<User> searchByAge(int age);
```

### Native SQL（直接寫真正的 SQL）

```java
@Query(value = "SELECT * FROM users WHERE name = :name", nativeQuery = true)
List<User> searchByNameNative(@Param("name") String name);
```

### 修改型查詢（UPDATE / DELETE）

寫入類的查詢必須加 `@Modifying`，通常也要 `@Transactional`。

```java
@Modifying
@Transactional
@Query("UPDATE User u SET u.status = :status WHERE u.id = :id")
int updateStatus(@Param("id") Long id, @Param("status") String status);
```

---

## 三、排序與分頁（Sort / Pageable）

這兩個是特殊參數，可以直接加在任何查詢方法後面。

```java
// 排序
List<User> findByStatus(String status, Sort sort);
// 呼叫端：findByStatus("active", Sort.by("createdAt").descending());

// 分頁
Page<User> findByStatus(String status, Pageable pageable);
// 呼叫端：findByStatus("active", PageRequest.of(0, 10));
```

`Page` 回傳物件會包含總筆數、總頁數等資訊；如果不需要總數，可以改回傳 `Slice` 效能較好。

---

## 新手容易踩雷的重點

這些是實務上容易出錯的地方，不是風格建議：

- JPQL 裡寫的是 **Entity 類別名與屬性名**，不是資料庫的 table 名與欄位名，這是最常搞混的地方。
- 方法命名查詢的欄位名必須對應 Entity 的**屬性名**，拼錯會在啟動時直接報錯。
- `@Modifying` 若忘了加，UPDATE/DELETE 的 `@Query` 會執行失敗。