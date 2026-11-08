import type { Category } from "../api/types";
import { CATEGORY_COLORS, CATEGORY_LABELS } from "../utils/format";

export default function CategoryBadge({ category }: { category: Category }) {
  return (
    <span className="badge">
      <span className="dot" style={{ background: CATEGORY_COLORS[category] }} />
      {CATEGORY_LABELS[category]}
    </span>
  );
}
