# Java Unit Test Developer — Mockito Patterns & AssertJ Assertions Reference

_Part of the `java-unit-test-developer` skill — see `../SKILL.md` for the topic index. Load this file only when your current task touches this topic._

## 5. Mockito Patterns

### Stubbing return values

```java
when(storageService.directoryExists("/photos")).thenReturn(true);
when(folderRepository.findByPath(any())).thenReturn(Optional.of(folder));
when(assetRepository.save(any(Asset.class))).thenAnswer(inv -> inv.getArgument(0));
```

### Stubbing void methods

```java
doNothing().when(thumbnailStorageService).saveThumbnail(anyLong(), any(byte[].class));
doThrow(new RuntimeException("disk full")).when(storageService).writeFile(any(), any());
```

### Verification

```java
verify(assetRepository, times(1)).save(any(Asset.class));
verify(folderRepository, never()).delete(any());
verify(storageService, atLeastOnce()).listImageFiles(any());
```

### ArgumentCaptor — inspect what was passed to a mock

```java
ArgumentCaptor<Asset> captor = ArgumentCaptor.forClass(Asset.class);
verify(assetRepository).save(captor.capture());
Asset saved = captor.getValue();
assertThat(saved.getFileName()).isEqualTo("photo.jpg");
assertThat(saved.getFolder()).isEqualTo(folder);
```

### argThat — inline predicate matching

```java
verify(folderRepository).save(argThat(f -> f.getPath().equals("/photos")));
```

### Throwing exceptions from stubs

```java
when(storageService.listImageFiles(any())).thenThrow(new IOException("disk error"));
```

---

## 6. AssertJ Assertions Reference

Always import `org.assertj.core.api.Assertions.assertThat`:

| Scenario                   | AssertJ assertion                                                             |
| -------------------------- | ----------------------------------------------------------------------------- |
| Not null                   | `assertThat(result).isNotNull()`                                              |
| Equality                   | `assertThat(result).isEqualTo(expected)`                                      |
| Boolean true/false         | `assertThat(flag).isTrue()` / `.isFalse()`                                    |
| List size                  | `assertThat(list).hasSize(3)`                                                 |
| List contains element      | `assertThat(list).contains(element)`                                          |
| List contains exactly      | `assertThat(list).containsExactly(a, b, c)`                                   |
| List is empty              | `assertThat(list).isEmpty()`                                                  |
| String contains            | `assertThat(str).contains("substring")`                                       |
| String starts with         | `assertThat(str).startsWith("prefix")`                                        |
| Optional is present        | `assertThat(opt).isPresent()`                                                 |
| Optional has value         | `assertThat(opt).hasValue(expected)`                                          |
| Exception thrown           | `assertThatThrownBy(() -> sut.method()).isInstanceOf(RuntimeException.class)` |
| Exception message          | `assertThatThrownBy(...).hasMessageContaining("disk error")`                  |
| Field value via extracting | `assertThat(asset).extracting(Asset::getFileName).isEqualTo("photo.jpg")`     |

---

