import { describe, expect, it } from 'vitest';
import { cloneTemplate, cloneWidget, createDefaultTemplate, createFilter, createWidgetByType, normalizeTemplateLayout, serializeTemplate } from './schema';

describe('schema utils', () => {
  it('creates a default template with one widget', () => {
    const template = createDefaultTemplate();

    expect(template.name).toBe('未命名报表');
    expect(template.schemaVersion).toBe('1.1.0');
    expect(template.versioning.state).toBe('draft');
    expect(template.versioning.draftVersion).toBe(1);
    expect(template.layout.gridColumns).toBe(24);
    expect(template.widgets).toHaveLength(1);
    expect(template.widgets[0].type).toBe('metric');
    expect(template.widgets[0].grid.w).toBe(12);
  });

  it('creates widgets by requested type', () => {
    const widget = createWidgetByType('bar');

    expect(widget.type).toBe('bar');
    expect(widget.title).toContain('柱状图');
    expect(widget.datasetId).toBe('cdm_patient');
    expect(widget.span).toBe(1);
  });

  it('creates table widgets with full-row span for mvp layout editing', () => {
    const widget = createWidgetByType('table');

    expect(widget.span).toBe(2);
  });

  it('creates filter rows with default equality operator', () => {
    const filter = createFilter();

    expect(filter.operator).toBe('eq');
    expect(filter.field).toBe('');
  });

  it('clones widgets with a new id and copied config', () => {
    const widget = createWidgetByType('bar');
    const cloned = cloneWidget(widget);

    expect(cloned.id).not.toBe(widget.id);
    expect(cloned.type).toBe(widget.type);
    expect(cloned.config).toEqual(widget.config);
    expect(cloned.title).toContain('副本');
  });

  it('serializes and clones without mutating source', () => {
    const source = createDefaultTemplate();
    const cloned = cloneTemplate(source);
    cloned.name = '复制模板';

    expect(source.name).toBe('未命名报表');
    expect(source.datasetCode).toBe('cdm_patient');
    expect(JSON.parse(serializeTemplate(source)).name).toBe('未命名报表');
  });

  it('normalizes widget grid layout and sort order', () => {
    const template = createDefaultTemplate();
    template.widgets = [createWidgetByType('table'), createWidgetByType('bar')];

    const normalized = normalizeTemplateLayout(template);

    expect(normalized.widgets[0].sortOrder).toBe(1);
    expect(normalized.widgets[0].grid.w).toBe(24);
    expect(normalized.widgets[1].sortOrder).toBe(2);
    expect(normalized.widgets[1].grid.y).toBeGreaterThanOrEqual(2);
  });

  it('preserves versioning metadata during normalization', () => {
    const template = createDefaultTemplate();
    template.status = 'PUBLISHED';
    template.versioning.state = 'published';
    template.versioning.publishedVersion = 2;
    template.versioning.effectiveVersion = 2;

    const normalized = normalizeTemplateLayout(template);

    expect(normalized.status).toBe('PUBLISHED');
    expect(normalized.versioning.state).toBe('published');
    expect(normalized.versioning.publishedVersion).toBe(2);
    expect(normalized.versioning.effectiveVersion).toBe(2);
  });
});
