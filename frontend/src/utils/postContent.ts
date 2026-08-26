import type { PostContentDocument, PostContentNode } from '@/types/postContent'

export function extractPostContentText(document: PostContentDocument): string {
  const text: string[] = []
  appendText(document.content ?? [], text)

  return text.join('').trim()
}

export function samePostContent(left: PostContentDocument, right: PostContentDocument): boolean {
  return JSON.stringify(left) === JSON.stringify(right)
}

function appendText(nodes: PostContentNode[], text: string[]) {
  for (const node of nodes) {
    if (node.type === 'text') {
      text.push(node.text ?? '')
      continue
    }

    if (node.type === 'hardBreak') {
      text.push('\n')
      continue
    }

    appendText(node.content ?? [], text)

    if (
      node.type === 'paragraph' ||
      node.type === 'heading' ||
      node.type === 'listItem' ||
      node.type === 'blockquote'
    ) {
      text.push('\n')
    }
  }
}
