package org.nr31.backend.exception;

import org.nr31.backend.dto.ErrorCode;
import org.nr31.backend.dto.cms.CmsValidationErrorResponse;
import org.nr31.backend.dto.cms.UpdateDraftRequest;
import org.nr31.backend.dto.cms.SlotDto;
import org.nr31.backend.dto.cms.WidgetDto;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CmsValidationHandler {

    private static final Pattern SLOT_WIDGET_FIELD_PATTERN = 
        Pattern.compile("^layoutData\\.slots\\[(\\d+)\\]\\.widgets\\[(\\d+)\\](?:\\.([a-zA-Z0-9_]+))?(?:\\..*)?$");
    private static final Pattern SLOT_FIELD_PATTERN = 
        Pattern.compile("^layoutData\\.slots\\[(\\d+)\\](?:\\.([a-zA-Z0-9_]+))?$");

    public static CmsValidationErrorResponse buildCmsValidationResponse(MethodArgumentNotValidException e) {
        UpdateDraftRequest request = (UpdateDraftRequest) e.getBindingResult().getTarget();
        Map<String, String> details = new LinkedHashMap<>();
        Map<String, Map<String, Object>> context = new LinkedHashMap<>();

        for (org.springframework.validation.ObjectError error : e.getBindingResult().getAllErrors()) {
            String key;
            String defaultMessage = error.getDefaultMessage();
            ParsedMessage pm = parseMessage(defaultMessage);

            if (error instanceof FieldError fieldError) {
                key = resolveCmsFieldKey(fieldError.getField(), request);
                enrichContextWithCmsMeta(pm.params, fieldError.getField(), request);
            } else {
                // Class-level validation constraints
                String codeName = error.getCode();
                if (codeName != null) {
                    if (codeName.equals("UniqueWidgetIds")) {
                        key = "layout:uniqueWidgetIds";
                        pm.key = "cms_validation.layout.duplicate_widget_ids";
                    } else if (codeName.equals("ValidAttachments")) {
                        key = "layout:attachments";
                        pm.key = "cms_validation.layout.invalid_attachments";
                    } else {
                        key = "layout:" + codeName.substring(0, 1).toLowerCase() + codeName.substring(1);
                    }
                } else {
                    key = "layout";
                }
            }

            details.put(key, pm.key);
            if (!pm.params.isEmpty()) {
                context.put(key, pm.params);
            }
        }

        return CmsValidationErrorResponse.builder()
                .message("Request validation failed")
                .code(ErrorCode.CMS_VALIDATION_ERROR)
                .details(details)
                .context(context.isEmpty() ? null : context)
                .timestamp(LocalDateTime.now())
                .build();
    }

    private static class ParsedMessage {
        String key;
        Map<String, Object> params = new HashMap<>();
    }

    private static ParsedMessage parseMessage(String defaultMessage) {
        ParsedMessage pm = new ParsedMessage();
        if (defaultMessage == null) {
            pm.key = "cms_validation.unknown";
            return pm;
        }

        if (defaultMessage.startsWith("cms_validation.")) {
            String[] parts = defaultMessage.split("\\|");
            pm.key = parts[0];
            for (int i = 1; i < parts.length; i++) {
                String[] kv = parts[i].split("=", 2);
                if (kv.length == 2) {
                    String val = kv[1];
                    try {
                        if (val.contains(".")) {
                            pm.params.put(kv[0], Double.parseDouble(val));
                        } else {
                            pm.params.put(kv[0], Long.parseLong(val));
                        }
                    } catch (NumberFormatException e) {
                        pm.params.put(kv[0], val);
                    }
                }
            }
        } else {
            pm.key = mapStandardMessage(defaultMessage);
            extractStandardParams(defaultMessage, pm.params);
        }
        return pm;
    }

    private static String mapStandardMessage(String msg) {
        if (msg == null) return "cms_validation.unknown";
        String lower = msg.toLowerCase();
        if (lower.contains("must not be blank") || lower.contains("must not be null") || lower.contains("must not be empty")) {
            return "cms_validation.field.required";
        }
        if (lower.contains("must be at least")) {
            return "cms_validation.field.min_value";
        }
        if (lower.contains("invite code must be a valid discord invite code")) {
            return "cms_validation.discord.invite_code_invalid";
        }
        if (lower.contains("channel id must be a valid youtube channel id")) {
            return "cms_validation.youtube.channel_id_invalid";
        }
        if (lower.contains("layout contains duplicate widget ids")) {
            return "cms_validation.layout.duplicate_widget_ids";
        }
        if (lower.contains("layout must contain at least one slot")) {
            return "cms_validation.slot.required";
        }
        if (lower.contains("slot must contain at least one widget")) {
            return "cms_validation.slot.required";
        }
        if (lower.contains("slot type must not be blank")) {
            return "cms_validation.slot.type_required";
        }
        if (lower.contains("layout contains invalid or unauthorized file attachments") || lower.contains("attached files do not exist")) {
            return "cms_validation.layout.invalid_attachments";
        }
        
        return "cms_validation.field.invalid_format";
    }

    private static void extractStandardParams(String msg, Map<String, Object> params) {
        if (msg == null) return;
        if (msg.contains("must be at least")) {
            Matcher m = Pattern.compile("must be at least (\\d+)").matcher(msg);
            if (m.find()) {
                params.put("min", Long.parseLong(m.group(1)));
            }
        }
    }

    private static String resolveCmsFieldKey(String fieldPath, UpdateDraftRequest request) {
        if (fieldPath == null || request == null || request.getLayoutData() == null) {
            return fieldPath;
        }

        Matcher widgetMatcher = SLOT_WIDGET_FIELD_PATTERN.matcher(fieldPath);
        if (widgetMatcher.matches()) {
            try {
                int slotIndex = Integer.parseInt(widgetMatcher.group(1));
                int widgetIndex = Integer.parseInt(widgetMatcher.group(2));
                String fieldName = widgetMatcher.group(3);

                if (request.getLayoutData().getSlots() != null && slotIndex < request.getLayoutData().getSlots().size()) {
                    SlotDto slot = request.getLayoutData().getSlots().get(slotIndex);
                    if (slot.getWidgets() != null && widgetIndex < slot.getWidgets().size()) {
                        WidgetDto widget = slot.getWidgets().get(widgetIndex);
                        if (widget.getId() != null) {
                            return "widget:" + widget.getId() + (fieldName != null ? ":" + fieldName : "");
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        Matcher slotMatcher = SLOT_FIELD_PATTERN.matcher(fieldPath);
        if (slotMatcher.matches()) {
            try {
                int slotIndex = Integer.parseInt(slotMatcher.group(1));
                String fieldName = slotMatcher.group(2);

                if (request.getLayoutData().getSlots() != null && slotIndex < request.getLayoutData().getSlots().size()) {
                    SlotDto slot = request.getLayoutData().getSlots().get(slotIndex);
                    if (slot.getSlotType() != null) {
                        return "slot:" + slot.getSlotType() + (fieldName != null && !"widgets".equals(fieldName) && !"slotType".equals(fieldName) ? ":" + fieldName : "");
                    }
                }
            } catch (Exception ignored) {
            }
        }

        return fieldPath;
    }

    private static void enrichContextWithCmsMeta(Map<String, Object> params, String fieldPath, UpdateDraftRequest request) {
        if (fieldPath == null || request == null || request.getLayoutData() == null) {
            return;
        }

        Matcher widgetMatcher = SLOT_WIDGET_FIELD_PATTERN.matcher(fieldPath);
        if (widgetMatcher.matches()) {
            try {
                int slotIndex = Integer.parseInt(widgetMatcher.group(1));
                int widgetIndex = Integer.parseInt(widgetMatcher.group(2));

                if (request.getLayoutData().getSlots() != null && slotIndex < request.getLayoutData().getSlots().size()) {
                    SlotDto slot = request.getLayoutData().getSlots().get(slotIndex);
                    params.put("slotType", slot.getSlotType());
                    if (slot.getWidgets() != null && widgetIndex < slot.getWidgets().size()) {
                        WidgetDto widget = slot.getWidgets().get(widgetIndex);
                        params.put("widgetId", widget.getId());
                        
                        String className = widget.getClass().getSimpleName();
                        String type = className.replace("WidgetDto", "").toLowerCase();
                        params.put("widgetType", type);
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }
}
