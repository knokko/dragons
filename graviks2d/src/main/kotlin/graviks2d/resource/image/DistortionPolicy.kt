package graviks2d.resource.image

enum class DistortionPolicy {
    /**
     * The image will always be drawn exactly within the given bounds, which will cause distortion if the
     * aspect ratio of the image is not equal to the aspect ratio of the bounds.
     */
    Allow,
    Scissor,
    Shrink
}
