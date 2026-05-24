import { describe, expect, it } from 'vitest';
import { cloneTemplate, createDefaultTemplate, createWidgetByType, serializeTemplate } from './schema';

describe('schema utils', () => {
  it('creates a default template with one widget', () => {
    const template = createDefaultTemplate();

    expect(template.name).toBe('未命名报表');
    expect(template.widgets).toHaveLength(1);
    expect(template.widgets[0].type).toBe('metric');
  });

  it('creates widgets by requested type', () => {
    const widget = createWidgetByType('bar');

    expect(widget.type).toBe('bar');
    expect(widget.title).toContain('柱状图');
  });

  it('serializes and clones without mutating source', () => {
    const source = createDefaultTemplate();
    const cloned = cloneTemplate(source);
    cloned.name = '复制模板';

    expect(source.name).toBe('未命名报表');
    expect(JSON.parse(serializeTemplate(source)).name).toBe('未命名报表');
  });
});
