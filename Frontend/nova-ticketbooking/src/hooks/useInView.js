import { useInView as useInViewLib } from "react-intersection-observer";
export function useInView(threshold = 0.1) {
  return useInViewLib({ threshold, triggerOnce: true });
}
