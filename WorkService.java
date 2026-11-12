package cn.edu.sdua.zhinengbook.service;

import cn.edu.sdua.zhinengbook.dto.Dtos;
import cn.edu.sdua.zhinengbook.entity.Work;
import cn.edu.sdua.zhinengbook.entity.WorkVersion;
import cn.edu.sdua.zhinengbook.exception.ApiException;
import cn.edu.sdua.zhinengbook.repository.WorkRepository;
import cn.edu.sdua.zhinengbook.repository.WorkVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 作品管理与创作历史版本管理
 */
@Service
@RequiredArgsConstructor
public class WorkService {

    private final WorkRepository workRepository;
    private final WorkVersionRepository versionRepository;

    public List<Map<String, Object>> listMine(Long userId) {
        return workRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(w -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", w.getId());
                    m.put("title", w.getTitle());
                    m.put("category", w.getCategory());
                    m.put("status", w.getStatus());
                    m.put("templateId", w.getTemplateId());
                    m.put("currentVersion", w.getCurrentVersion());
                    m.put("updatedAt", w.getUpdatedAt());
                    m.put("preview", w.getContent() == null ? "" :
                            w.getContent().length() > 80 ? w.getContent().substring(0, 80) + "…" : w.getContent());
                    return m;
                }).toList();
    }

    public Work getOwned(Long userId, Long workId) {
        Work work = workRepository.findById(workId)
                .orElseThrow(() -> new ApiException(404, "作品不存在"));
        if (!work.getUserId().equals(userId)) {
            throw new ApiException(403, "无权访问该作品");
        }
        return work;
    }

    public Map<String, Object> detail(Long userId, Long workId) {
        Work w = getOwned(userId, workId);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", w.getId());
        m.put("title", w.getTitle());
        m.put("content", w.getContent());
        m.put("category", w.getCategory());
        m.put("status", w.getStatus());
        m.put("templateId", w.getTemplateId());
        m.put("currentVersion", w.getCurrentVersion());
        m.put("createdAt", w.getCreatedAt());
        m.put("updatedAt", w.getUpdatedAt());
        return m;
    }

    /** 手动保存作品（新建或更新，产生 EDIT 版本） */
    @Transactional
    public Work save(Long userId, Dtos.WorkSaveRequest req) {
        Work work;
        if (req.id() != null) {
            work = getOwned(userId, req.id());
            if (req.content() != null && !req.content().equals(work.getContent())) {
                work.setContent(req.content());
                work.setUpdatedAt(LocalDateTime.now());
                workRepository.save(work);
                addVersion(work.getId(), req.content(), "EDIT",
                        req.note() == null ? "手动编辑保存" : req.note(), userId);
            }
        } else {
            work = new Work();
            work.setUserId(userId);
            work.setTitle(req.title());
            work.setContent(req.content());
            work.setTemplateId(req.templateId());
            work.setCategory(req.category());
            workRepository.save(work);
            if (req.content() != null && !req.content().isBlank()) {
                addVersion(work.getId(), req.content(), "EDIT",
                        req.note() == null ? "创建作品" : req.note(), userId);
            }
        }
        return work;
    }

    /** AI生成/润色后落库（由GenerationService调用） */
    @Transactional
    public Work createGenerated(Long userId, String title, String content,
                                Long templateId, String category, String changeType, String note) {
        Work work = new Work();
        work.setUserId(userId);
        work.setTitle(title);
        work.setContent(content);
        work.setTemplateId(templateId);
        work.setCategory(category);
        workRepository.save(work);
        addVersion(work.getId(), content, changeType, note, userId);
        return work;
    }

    @Transactional
    public void updateContent(Work work) {
        work.setUpdatedAt(LocalDateTime.now());
        workRepository.save(work);
    }

    @Transactional
    public void addVersion(Long workId, String content, String changeType, String note, Long operatorId) {
        int no = versionRepository.maxVersionNo(workId) + 1;
        WorkVersion v = new WorkVersion();
        v.setWorkId(workId);
        v.setVersionNo(no);
        v.setContent(content);
        v.setChangeType(changeType);
        v.setChangeNote(note);
        v.setOperatorId(operatorId);
        versionRepository.save(v);
        workRepository.findById(workId).ifPresent(w -> {
            w.setCurrentVersion(no);
            w.setUpdatedAt(LocalDateTime.now());
            workRepository.save(w);
        });
    }

    public List<Map<String, Object>> versions(Long userId, Long workId) {
        getOwned(userId, workId);
        return versionRepository.findByWorkIdOrderByVersionNoDesc(workId).stream().map(v -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", v.getId());
            m.put("versionNo", v.getVersionNo());
            m.put("changeType", v.getChangeType());
            m.put("changeNote", v.getChangeNote());
            m.put("createdAt", v.getCreatedAt());
            m.put("length", v.getContent() == null ? 0 : v.getContent().length());
            return m;
        }).toList();
    }

    public String versionContent(Long userId, Long workId, Integer versionNo) {
        getOwned(userId, workId);
        return versionRepository.findByWorkIdOrderByVersionNoDesc(workId).stream()
                .filter(v -> v.getVersionNo().equals(versionNo))
                .findFirst()
                .map(WorkVersion::getContent)
                .orElseThrow(() -> new ApiException(404, "版本不存在"));
    }

    /** 回滚到指定版本（生成 RESTORE 新版本，历史不丢失） */
    @Transactional
    public void restore(Long userId, Long workId, Integer versionNo) {
        getOwned(userId, workId);
        String content = versionContent(userId, workId, versionNo);
        workRepository.findById(workId).ifPresent(w -> {
            w.setContent(content);
            workRepository.save(w);
        });
        addVersion(workId, content, "RESTORE", "回滚至版本 v" + versionNo, userId);
    }

    @Transactional
    public void delete(Long userId, Long workId) {
        Work work = getOwned(userId, workId);
        versionRepository.findByWorkIdOrderByVersionNoDesc(workId)
                .forEach(v -> versionRepository.delete(v));
        workRepository.delete(work);
    }

    @Transactional
    public void publish(Long userId, Long workId) {
        Work work = getOwned(userId, workId);
        work.setStatus("PUBLISHED".equals(work.getStatus()) ? "DRAFT" : "PUBLISHED");
        work.setUpdatedAt(LocalDateTime.now());
        workRepository.save(work);
    }
}
