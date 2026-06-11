import { describe, it, expect } from "vitest";
import {
  getTicketStatusLabel,
  getTicketPriorityLabel,
  getTicketTypeLabel,
  getTicketCategoryLabel,
  ticketStatusOptions,
  ticketPriorityOptions,
  ticketTypeOptions
} from "./tickets";

describe("tickets constants", () => {
  describe("getTicketStatusLabel", () => {
    it("returns correct label for status 1 (新建)", () => {
      expect(getTicketStatusLabel(1)).toBe("新建");
    });
    it("returns correct label for status 2 (处理中)", () => {
      expect(getTicketStatusLabel(2)).toBe("处理中");
    });
    it("returns correct label for status 3 (已解决)", () => {
      expect(getTicketStatusLabel(3)).toBe("已解决");
    });
    it("returns correct label for status 4 (已关闭)", () => {
      expect(getTicketStatusLabel(4)).toBe("已关闭");
    });
    it("returns fallback for unknown status", () => {
      expect(getTicketStatusLabel(99)).toBe("未知状态");
    });
    it("returns fallback for null", () => {
      expect(getTicketStatusLabel(null)).toBe("未知状态");
    });
    it("returns fallback for undefined", () => {
      expect(getTicketStatusLabel(undefined)).toBe("未知状态");
    });
  });

  describe("getTicketPriorityLabel", () => {
    it("returns correct label for P4", () => {
      expect(getTicketPriorityLabel(4)).toBe("P4 - 紧急");
    });
    it("returns correct label for P1", () => {
      expect(getTicketPriorityLabel(1)).toBe("P1 - 低");
    });
    it("returns fallback for unknown", () => {
      expect(getTicketPriorityLabel(99)).toBe("未配置");
    });
  });

  describe("getTicketTypeLabel", () => {
    it("returns 故障 for INCIDENT", () => {
      expect(getTicketTypeLabel("INCIDENT")).toBe("故障");
    });
    it("returns 咨询 for QUESTION", () => {
      expect(getTicketTypeLabel("QUESTION")).toBe("咨询");
    });
    it("returns 其他 for unknown type", () => {
      expect(getTicketTypeLabel("UNKNOWN")).toBe("其他");
    });
  });

  describe("getTicketCategoryLabel", () => {
    it("returns 使用支持 for category 1", () => {
      expect(getTicketCategoryLabel(1)).toBe("使用支持");
    });
    it("returns 未分类 for unknown", () => {
      expect(getTicketCategoryLabel(99)).toBe("未分类");
    });
  });

  describe("options arrays", () => {
    it("has 4 status options", () => {
      expect(ticketStatusOptions).toHaveLength(4);
    });
    it("has 4 priority options", () => {
      expect(ticketPriorityOptions).toHaveLength(4);
    });
    it("has 3 type options", () => {
      expect(ticketTypeOptions).toHaveLength(3);
    });
  });
});
