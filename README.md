# fossilphant

### a static-site generator for mastodon archives

fossilphant generates static websites from
archives of your posts [you can export](https://allthings.how/how-to-export-and-import-your-data-on-mastodon/)
from a Mastodon instance.

If your instance is shutting down, you can move to a new server,
but your posts as a member of the departing instance will be effectively
deleted from the internet.

To keep your posts alive...

1. download an archive of your posts
2. use `fossilphant` to generate simple, static website
   from the archive
3. rehost the site via any platform or service that supports simple websites

## Examples

You can see examples in the two themes currently defined,
[`shatter`](https://www.mchange.com/projects/fossilphant/example/shatter)
and [`tower`](https://www.mchange.com/projects/fossilphant/example/tower).

`shatter` paginates your posts. `tower` places them all on one giant webpage.
They are visually very similar.

Both themes collect your threads together, and display them in forward-chronological
order (against the general reverse-chronological grain).

Both offer a "main" view that
excludes your replies to other people's posts, and a posts-with-replies view that
includes those.

"Boosts" are not republished.

## Quick Start

### scala-cli script (mac, linux)

You will need [scala-cli](https://scala-cli.virtuslab.org/) installed on your machine.
On a mac with homebrew that's just
```plaintext
$ brew install Virtuslab/scala-cli/scala-cli
```
Then...
1. Download your archive file from your Mastodon instance
2. Download the script [`fossilphant`](https://github.com/swaldman/fossilphant/releases/download/v0.0.1/fossilphant)
3. Make it executable
   ```plaintext
   $ chmod ugo+x fossilphant
   ```
4. Run the script
   ```plaintext
   $ ./fossilphant /path/to/my-mastodon-archive
   ```
   If you want posts that you marked sensitive
   and/or followers-only posts included, then
   ```plaintext
   $ ./fossilphant --include-followers-only --include-sensitive /path/to/my-mastodon-archive
   ```
   The archive can be an already extracted directory, or the original
    `.tar.gz` file. (The application will extract the compressed archive into a
   temporary directory if necessary.)

You should see a directory called `public` that contains your new website.

To customize, run
   ```plaintext
   $ ./fossilphant --help
   ```
and check out the many options!

### scala-cli script (windows, any scala-cli supported platform)

You will need [scala-cli](https://scala-cli.virtuslab.org/) installed on your machine.

1. Download your archive file from your Mastodon instance
2. Download the script [`fossilphant.sc`](https://github.com/swaldman/fossilphant/releases/download/v0.0.1/fossilphant.sc)
3. Run the script
   ```plaintext
   C:\Users\steve>scala-cli fossilphant.sc /path/to/my-mastodon-archive
   ```
   If you want posts that you marked sensitive
   and/or followers-only posts included, then
   ```plaintext
   C:\Users\steve>scala-cli fossilphant.sc --include-followers-only --include-sensitive /path/to/my-mastodon-archive
   ```
   The archive can be an already extracted directory, or the original
    `.tar.gz` file. (The application will extract the compressed archive into a
   temporary directory if necessary.)

You should see a directory called `public` that contains your new website.

To customize, run
   ```plaintext
   C:\Users\steve>scala-cli fossilphant.sc --help
   ```
and check out the many options!

### old school

You will need a [Java virtual machine](https://www.oracle.com/java/technologies/java-se-glance.html),
version 17 or higher, installed on your machine, and a UNIXy command line.

1. Download your archive file from your Mastodon instance
2. Clone or download this distribution
3. From the root directory of this distribution, run
   ```plaintext
   $ ./fossilphant-site-gen /path/to/my-mastodon-archive
   ```
   The archive can be an already extracted directory, or the original
    `.tar.gz` file. (The application will extract the compressed archive into a
   temporary directory if necessary.)

That's it!

You should see a directory called `public`
that contains your new website.

## Privacy

At least for now, the full `/media_attachments` directory, containing all of the images and other media
you might have posted, is copied into the website. This directory might include images associated
with nonpublic posts.

Though it might be hard to guess the image file paths, they'll be there on a public
webserver for anyone who might have those paths, including recipients of the original private
posts, administrators of their servers, and whomever they talk to. And be sure that to set [`autoindex`](https://docs.nginx.com/nginx/admin-guide/web-server/serving-static-content/)
or its equivalent to `off` so that snoopers can't just browse your media library.

> [!NOTE]  
> _Posts that were sent followers-only or that are marked sensitive
> will not be published by default. Edit `config.scala` if you want
> those posts published._
> 
> _(Posts directed neither to the general public
> nor to followers will not be published. No setting overrides that.
> Although of course a bug might.)_


## Customization

`fossilphant` is configurable, and for the truly ambitious,
themable.

Check out the file [`fossilphant/src/config.scala`](fossilphant/src/config.scala) to customize.
Configuration is documented in the comments of that file. Just edit it in place.

You can mess around with colors, direct self links to your new identity,
redirect tag links to a new host so they don't break against a defunct instance,
change the title, the tagline, or the whole theme.

## Theme notes

### shatter

Although `shatter` is based around paged feeds linking to single-post pages,
it also includes an unlinked-from-index endpoint `/withrepliesSinglePage.html` which
(`tower`-like) shows all posts and replies in one very tall page.

The motivation for this that while `shatter`'s paging may be good for normal
browsing, it's nice to be able to search your posts just by `<ctrl>` or `<command> F`.
`/withrepliesSinglePage.html` is emitted for that!

### tower

_No notes yet!_

## Development

`fossilphant` is built in Scala, on top of my own template (well,
[`untemplate`](https://github.com/swaldman/untemplate-doc#readme)) library,
and my static-site-generator generator [`unstatic`](https://github.com/swaldman/unstatic).

If you are a front-end developer, the
[templates that define themes](fossilphant/untemplate/com/mchange/fossilphant/theme)
should be somewhat familiar, below headers of Scala code. (Theme `tower` is more accessible.) It's mostly
[HTML](fossilphant/untemplate/com/mchange/fossilphant/theme/tower/layout-main.html.untemplate) and
[CSS](fossilphant/untemplate/com/mchange/fossilphant/theme/tower/style.css.gen.untemplate), should be pretty tweakble.

For the ambitious, themes are defined by untemplates ending in
* `.gen.untemplate` (one template generated per site)
* `.genpost.untemplate`
(many templates generated, one per post)
* `.genpagewithall.untemplate` (several templates generated, one per page of paginated posts including all replies)
* `.genpagewithothers.untemplate` (several templates generated, one per page of paginated posts including replies only to others)
* `.genpagewithout.untemplate` (several templates generated, one per page of paginated posts, excluding replies)

Templates without a `.gen*.untemplate` suffix define helper functions,
used by the root, autogenerated templates.

Every untemplate defines a scala function. The helpers may be defined
to accept pretty much whatever arguments, _ad hoc_. (They each accept a
single argument, but that can be a tuple, rendering them callable with
multiple args.)

The root untemplates accept

| Root                            | Type                     |
|---------------------------------|--------------------------|
| `.gen.untemplate`               | `LocatedContext`         |
| `.getpost.untemplate`           | `LocatedPostWithContext` | 
| `.genpagewithall.untemplate`    | `LocatedPageWithContext` |
| `.genpagewithothers.untemplate` | `LocatedPageWithContext` |
| `.genpagewithout.untemplate`    | `LocatedPageWithContext` |

The template-generated functions live in Scala packages, beneath `com.mchange.fossilphant.theme`.

Each theme also includes helper functions, as well as constructs written in normal Scala.
You'll find those under the [parallel Scala source packages](fossilphant/src/com/mchange/fossilphant/theme).

If you are interested in learning more about these libraries and tools, hit me up.

## Credits

Site generation is built atop the very excellent [mill](https://github.com/com-lihaoyi/mill).

Libraries of note beneath this include [zio](https://zio.dev/), [tapir](https://tapir.softwaremill.com/en/latest/),
[os-lib](https://github.com/com-lihaoyi/os-lib), [upickle](https://com-lihaoyi.github.io/upickle/),
[JArchiveLib](https://rauschig.org/jarchivelib/), [scopt](https://github.com/scopt/scopt) and undoubtedly many more.
